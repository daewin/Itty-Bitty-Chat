package com.daewin.ibachat.chat;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import com.daewin.ibachat.model.ThreadModel;
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.timestamp.TimestampInterpreter;
import com.daewin.ibachat.user.User;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

/**
 */

public class ChatActivity extends AppCompatActivity {

    public static final String ARG_NAME_OF_FRIEND = "NAME_OF_FRIEND";
    public static final String ARG_EMAIL_OF_FRIEND = "EMAIL_OF_FRIEND";
    public static final String ARG_CURRENT_THREAD_ID = "CURRENT_THREAD_ID";
    private static final int THREAD_MESSAGES_LIMIT = 50;

    // Database references
    private DatabaseReference statusOfFriendReference;
    private DatabaseReference typingStateOfFriendReference;
    private DatabaseReference typingStateOfUserReference;
    private DatabaseReference lastSeenOfFriendReference;
    private DatabaseReference threadMessagesReference;
    private DatabaseReference friendsThreadReference;
    private DatabaseReference usersThreadReference;

    // Listeners
    private ValueEventListener statusOfFriendListener;
    private ValueEventListener typingStateOfFriendListener;
    private ChildEventListener threadMessagesListener;

    private ChatActivityBinding binding;
    private TextView statusTextView;
    private ArrayList<MessageModel> messages;
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
        initializeRecyclerView(threadID);
        initializeTypingState();
        initializeSendButtonBehaviour(threadID);
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


        String encodedEmailOfFriend
                = User.getEncodedEmail(emailOfFriend);

        currentUser = User.getCurrentUserModel();

        if (currentUser != null) {
            String currentUsersEncodedEmail = currentUser.getEncodedEmail();

            // Friend reference initialization
            typingStateOfFriendReference
                    = currentThreadReference.child("members")
                    .child(encodedEmailOfFriend)
                    .child("is_typing");

            statusOfFriendReference
                    = databaseReference.child("users")
                    .child(encodedEmailOfFriend)
                    .child("connections");

            lastSeenOfFriendReference
                    = databaseReference.child("users")
                    .child(encodedEmailOfFriend)
                    .child("lastSeen");

            friendsThreadReference = databaseReference.child("users")
                    .child(encodedEmailOfFriend)
                    .child("user_threads_with")
                    .child(currentUsersEncodedEmail);

            // Current user reference initialization
            typingStateOfUserReference
                    = currentThreadReference.child("members")
                    .child(currentUsersEncodedEmail)
                    .child("is_typing");

            usersThreadReference = databaseReference.child("users")
                    .child(currentUsersEncodedEmail)
                    .child("user_threads_with")
                    .child(encodedEmailOfFriend);
        }

        // Thread messages reference initialization
        threadMessagesReference
                = databaseReference.child("thread_messages").child(threadID);
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

    private void initializeRecyclerView(String threadID) {
        mRecyclerView = binding.chatRecyclerView;
        messages = new ArrayList<>();

        // Specify our adapter
        chatListAdapter = new ChatListAdapter(messages, threadID);
        mRecyclerView.setAdapter(chatListAdapter);
        mRecyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);

        mRecyclerView.setLayoutManager(mLayoutManager);

        // To allow endless scrolling (not working atm.)
        //mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
        //
        //    @Override
        //    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        //        int lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();
        //        int totalMessagesCount = mLayoutManager.getItemCount();
        //
        //        // Once we reach the top of the list, load more if any
        //        if(moreMessagesToBeLoaded){
        //            if ((lastVisibleItemPosition + 1 == totalMessagesCount)) {
        //                System.err.println("Loading more..." + mThreadMessageLimit);
        //                mThreadMessageLimit += THREAD_MESSAGES_LIMIT;
        //                loadThreadMessages(mThreadMessageLimit);
        //                moreMessagesToBeLoaded = false;
        //            }
        //        }
        //    }
        //});
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

    private void initializeSendButtonBehaviour(final String threadID) {

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

                    // According to the docs, sorting a nearly-sorted list (this as it builds up)
                    // requires only n comparisons, which is acceptable.
                    Collections.sort(messages, MessageModel.timeComparator);
                    chatListAdapter.notifyItemInserted(messages.indexOf(messageModel));
                    mRecyclerView.smoothScrollToPosition(0);

                    // Do this after adding locally to avoid double messages
                    threadMessagesReference.push().setValue(messageModel);

                    binding.chatMessageArea.setText("");

                    // Push this message to both the user's thread nodes
                    ThreadModel thread
                            = new ThreadModel(threadID, currentUser.getName(), message, timestamp);

                    friendsThreadReference.setValue(thread);
                    usersThreadReference.setValue(thread);
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

        loadThreadMessages();
    }

    private void loadThreadMessages() {

        if (threadMessagesListener != null) {
            // Detach the old listener if it exists
            threadMessagesQuery.keepSynced(false);
            threadMessagesQuery.removeEventListener(threadMessagesListener);
        }

        //threadMessagesQuery = threadMessagesReference.limitToFirst(threadMessageLimit);
        threadMessagesQuery = threadMessagesReference;

        threadMessagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                MessageModel incomingMessage = dataSnapshot.getValue(MessageModel.class);

                if (incomingMessage != null) {

                    // Since it's "live data", set the message ID to the push key
                    incomingMessage.setMessageID(dataSnapshot.getKey());

                    // To allow our local data to sync up properly, we check if the incoming
                    // message is "equals" (based on the email, message and timestamp) to the
                    // local message.
                    if (messages.contains(incomingMessage)) {
                        int storedMessageIndex = messages.indexOf(incomingMessage);
                        MessageModel storedMessage = messages.get(storedMessageIndex);

                        // We set the message ID, but we don't have to notify to avoid unnecessary
                        // layout redraws. Any changes will be handled in onChildChanged
                        storedMessage.setMessageID(incomingMessage.getMessageID());
                        chatListAdapter.notifyItemChanged(storedMessageIndex);
                        return;
                    }

                    messages.add(incomingMessage);

                    // According to the docs, sorting a nearly-sorted list (this as it builds up)
                    // requires only n comparisons, which is acceptable.
                    Collections.sort(messages, MessageModel.timeComparator);
                    chatListAdapter.notifyItemInserted(messages.indexOf(incomingMessage));

                    mRecyclerView.smoothScrollToPosition(0);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // Currently, only the "seen" status can be changed
                MessageModel incomingMessage = dataSnapshot.getValue(MessageModel.class);

                if (incomingMessage != null) {
                    if (messages.contains(incomingMessage)) {
                        int storedMessageIndex = messages.indexOf(incomingMessage);
                        MessageModel storedMessage = messages.get(storedMessageIndex);

                        storedMessage.setSeen(incomingMessage.isSeen());
                        chatListAdapter.notifyItemChanged(storedMessageIndex);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Removing messages is not currently supported
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                // Shouldn't happen
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        };

        // Firebase has been set to cache the Queries, so that future calls would not
        // have to reload the same messages (hopefully)
        threadMessagesQuery.keepSynced(true);
        threadMessagesQuery.addChildEventListener(threadMessagesListener);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
