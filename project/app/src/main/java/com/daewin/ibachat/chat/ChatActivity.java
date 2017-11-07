package com.daewin.ibachat.chat;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.daewin.ibachat.MyLifecycleObserver;
import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.ChatActivityBinding;
import com.daewin.ibachat.model.MessageModel;
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.timestamp.TimestampInterpreter;
import com.daewin.ibachat.user.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;

/**
 */

public class ChatActivity extends AppCompatActivity {

    public static final String ARG_NAME_OF_FRIEND = "NAME_OF_FRIEND";
    public static final String ARG_EMAIL_OF_FRIEND = "EMAIL_OF_FRIEND";
    public static final String ARG_CURRENT_THREAD_ID = "CURRENT_THREAD_ID";
    private static final int INITIAL_THREAD_MESSAGES_LIMIT = 50;

    // Database references
    private DatabaseReference statusOfFriendReference;
    private DatabaseReference typingStateOfFriendReference;
    private DatabaseReference typingStateOfUserReference;
    private DatabaseReference lastSeenOfFriendReference;
    private DatabaseReference threadMessagesReference;
    private DatabaseReference friendSeenMessagesReference;

    // Listeners
    private ValueEventListener statusOfFriendListener;
    private ValueEventListener typingStateOfFriendListener;
    private ValueEventListener threadMessagesListener;

    private ChatActivityBinding binding;
    private TextView statusTextView;
    private ArrayList<MessageModel> messages;
    private Queue<ArrayList<MessageModel>> pendingUpdates;
    private ChatListAdapter chatListAdapter;
    private Query threadMessagesQuery;
    private RecyclerView mRecyclerView;
    private UserModel currentUser;
    private boolean friendsStatusSet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new MyLifecycleObserver());

        binding = DataBindingUtil.setContentView(this, R.layout.chat_activity);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        String nameOfFriend = getIntent().getStringExtra(ARG_NAME_OF_FRIEND);
        String emailOfFriend = getIntent().getStringExtra(ARG_EMAIL_OF_FRIEND);
        String threadID = getIntent().getStringExtra(ARG_CURRENT_THREAD_ID);

        initializeDatabase(emailOfFriend, threadID);
        initializeActionBar(nameOfFriend);
        initializeRecyclerView();
        initializeTypingState();
        initializeSendButtonBehaviour();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeStatusOfFriendListener();
        initializeThreadMessagesEventListener();
        friendsStatusSet = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        setTypingState(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (statusOfFriendListener != null) {
            statusOfFriendReference.removeEventListener(statusOfFriendListener);
        }

        if (typingStateOfFriendListener != null) {
            typingStateOfFriendReference.removeEventListener(typingStateOfFriendListener);
        }

        if (threadMessagesListener != null) {
            threadMessagesReference.removeEventListener(threadMessagesListener);
        }

        threadMessagesQuery.keepSynced(false);
    }

    private void initializeDatabase(String emailOfFriend, String threadID) {
        DatabaseReference databaseReference
                = FirebaseDatabase.getInstance().getReference();

        DatabaseReference currentThreadReference
                = databaseReference.child("threads").child(threadID);

        // Friend reference initialization
        String encodedEmailOfFriend
                = User.getEncodedEmail(emailOfFriend);

        typingStateOfFriendReference
                = currentThreadReference.child("members")
                .child(encodedEmailOfFriend)
                .child("is_typing");

        friendSeenMessagesReference
                = currentThreadReference.child("members")
                .child(encodedEmailOfFriend)
                .child("seen");

        statusOfFriendReference
                = databaseReference.child("users")
                .child(encodedEmailOfFriend)
                .child("connections");

        lastSeenOfFriendReference
                = databaseReference.child("users")
                .child(encodedEmailOfFriend)
                .child("lastSeen");


        // Current user reference initialization
        currentUser = User.getCurrentUserModel();

        if (currentUser != null) {
            String currentUsersEncodedEmail = currentUser.getEncodedEmail();

            typingStateOfUserReference
                    = currentThreadReference.child("members")
                    .child(currentUsersEncodedEmail)
                    .child("is_typing");
        }

        // Thread messages reference initialization
        threadMessagesReference
                = databaseReference.child("thread_messages")
                .child(threadID);
    }

    private void initializeActionBar(String nameOfFriend) {
        setSupportActionBar(binding.chatToolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.chat_action_bar);

            View customActionBar = actionBar.getCustomView();

            TextView nameTextView = customActionBar.findViewById(R.id.nameTextView);
            statusTextView = customActionBar.findViewById(R.id.statusTextView);

            nameTextView.setText(nameOfFriend);
        }
    }

    private void initializeRecyclerView() {
        mRecyclerView = binding.chatRecyclerView;
        messages = new ArrayList<>();
        pendingUpdates = new ArrayDeque<>();

        // Specify our adapter
        chatListAdapter = new ChatListAdapter(messages);
        mRecyclerView.setAdapter(chatListAdapter);
        mRecyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);

        mRecyclerView.setLayoutManager(mLayoutManager);
    }

    private void initializeTypingState() {
        EditText chatMessageArea = binding.chatMessageArea;
        chatMessageArea.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.length() == 0) {
                    setTypingState(false);
                } else {
                    setTypingState(true);
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do nothing
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do nothing
            }
        });
    }

    private void setTypingState(boolean isTyping) {
        typingStateOfUserReference.setValue(isTyping);
    }

    private void initializeSendButtonBehaviour() {

        binding.chatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = binding.chatMessageArea.getText().toString();

                if (!message.isEmpty()) {

                    String email = currentUser.getEmail();
                    Long timestamp = System.currentTimeMillis();

                    final MessageModel messageModel
                            = new MessageModel(email, message, timestamp);

                    // Add it locally to handle offline situations. Firebase handles the pushes
                    // locally too until it reconnects with the database, which it then syncs after
                    messages.add(messageModel);
                    chatListAdapter.notifyItemInserted(messages.indexOf(messageModel));

                    threadMessagesReference.push().setValue(messageModel);
                    binding.chatMessageArea.setText("");
                }
            }
        });
    }

    private void initializeStatusOfFriendListener() {
        statusOfFriendListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    if (!friendsStatusSet) {
                        // Since a user can have multiple devices, we only set it to online for
                        // the first "online" status update. This is to prevent multiple listeners
                        // set for the typing activity.
                        statusTextView.setText(R.string.user_online);

                        // Initialize listener for their typing activity
                        initializeTypingStateOfFriendListener();

                        friendsStatusSet = true;
                    }
                } else {
                    // Friend is offline, so we remove the typing activity listener
                    if (typingStateOfFriendListener != null) {
                        typingStateOfFriendReference
                                .removeEventListener(typingStateOfFriendListener);
                    }

                    setLastSeenOfFriend();
                    friendsStatusSet = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        };

        statusOfFriendReference.addValueEventListener(statusOfFriendListener);
    }

    private void initializeTypingStateOfFriendListener() {
        typingStateOfFriendListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Boolean isTyping = dataSnapshot.getValue(Boolean.class);

                if (isTyping != null) {
                    if (isTyping) {
                        statusTextView.setText(R.string.is_typing);
                    } else {
                        // We assume that if a client had sent a "false" value for isTyping,
                        // and this listener was triggered, they'd still be online to do so.
                        statusTextView.setText(R.string.user_online);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        };

        typingStateOfFriendReference.addValueEventListener(typingStateOfFriendListener);
    }

    private void setLastSeenOfFriend() {
        lastSeenOfFriendReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Long lastSeen = dataSnapshot.getValue(Long.class);

                if (lastSeen != null) {
                    TimestampInterpreter interpreter = new TimestampInterpreter(lastSeen);
                    String interpretation = interpreter.getTimestampInterpretation();

                    switch (interpretation) {
                        case TimestampInterpreter.TODAY:
                            statusTextView
                                    .setText(String.format("Last seen today at %s",
                                            interpreter.getTime()));
                            break;
                        case TimestampInterpreter.YESTERDAY:
                            statusTextView
                                    .setText(String.format("Last seen yesterday at %s",
                                            interpreter.getTime()));
                            break;
                        default:
                            statusTextView
                                    .setText(String.format("Last seen: %s",
                                            interpreter.getFullDate()));
                            break;
                    }

                } else {
                    statusTextView.setText(R.string.user_inactive);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        });
    }

    private void initializeThreadMessagesEventListener() {

        loadThreadMessages(INITIAL_THREAD_MESSAGES_LIMIT);
    }

    private void loadThreadMessages(int threadMessageLimit) {

        if (threadMessagesListener != null) {
            // Detach the old listener if it exists
            threadMessagesQuery.keepSynced(false);
            threadMessagesQuery.removeEventListener(threadMessagesListener);
        }

        threadMessagesQuery = threadMessagesReference.limitToFirst(threadMessageLimit);

        threadMessagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<MessageModel> newMessages = new ArrayList<>();

                for (DataSnapshot messagesSnapshot : dataSnapshot.getChildren()) {
                    MessageModel incomingMessage = messagesSnapshot.getValue(MessageModel.class);

                    if (incomingMessage != null) {
                        newMessages.add(incomingMessage);
                    }
                }

                // Sort according to time
                Collections.sort(newMessages, MessageModel.timeComparator);
                updateMessagesList(newMessages);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        };

        // Firebase has been set to cache the Queries, so that future calls would not
        // have to reload the same messages (hopefully)
        threadMessagesQuery.keepSynced(true);
        threadMessagesQuery.addValueEventListener(threadMessagesListener);
    }

    // When new data becomes available or the thread message limit has been extended, this method
    // handles updating the messages list. We use a queue to allow concurrent updates, if the
    // previous diffUtil call hasn't completed generating the diffResult yet.
    // Based off: https://medium.com/@jonfhancock/get-threading-right-with-diffutil-423378e126d2
    private void updateMessagesList(ArrayList<MessageModel> newMessages) {
        pendingUpdates.add(newMessages);

        if (pendingUpdates.size() > 1) {
            // One at a time.
            return;
        }
        updateMessagesListInternal(newMessages);
    }

    // This method does the heavy lifting of pushing the work to the background thread.
    private void updateMessagesListInternal(final ArrayList<MessageModel> newMessages) {
        final ArrayList<MessageModel> oldMessages = new ArrayList<>(messages);
        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                MyDiffCallback myDiffCallback = new MyDiffCallback(oldMessages, newMessages);

                final DiffUtil.DiffResult diffResult
                        = DiffUtil.calculateDiff(myDiffCallback, false);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        applyDiffResult(newMessages, diffResult);
                    }
                });
            }
        }).start();
    }

    // This method is called when the background work is done
    private void applyDiffResult(ArrayList<MessageModel> newMessages,
                                 DiffUtil.DiffResult diffResult) {

        pendingUpdates.remove();
        dispatchUpdates(newMessages, diffResult);

        if (pendingUpdates.size() > 0) {
            updateMessagesListInternal(pendingUpdates.peek());
        }
    }

    private void dispatchUpdates(ArrayList<MessageModel> newMessages,
                                 DiffUtil.DiffResult diffResult) {

        messages.clear();
        messages.addAll(newMessages);
        diffResult.dispatchUpdatesTo(chatListAdapter);
        mRecyclerView.smoothScrollToPosition(0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    private class MyDiffCallback extends DiffUtil.Callback {
        ArrayList<MessageModel> oldList;
        ArrayList<MessageModel> newList;

        MyDiffCallback(ArrayList<MessageModel> oldList, ArrayList<MessageModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return areContentsTheSame(oldItemPosition, newItemPosition);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            MessageModel oldModel = oldList.get(oldItemPosition);
            MessageModel newModel = newList.get(newItemPosition);

            return oldModel.getEmail().equals(newModel.getEmail())
                    && oldModel.getMessage().equals(newModel.getMessage())
                    && oldModel.getTimestamp().equals(newModel.getTimestamp());
        }
    }
}
