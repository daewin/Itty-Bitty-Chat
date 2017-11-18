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
import com.daewin.ibachat.database.DatabaseUtil;
import com.daewin.ibachat.databinding.ChatActivityBinding;
import com.daewin.ibachat.model.MessageModel;
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.timestamp.TimestampInterpreter;
import com.daewin.ibachat.user.User;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

/**
 * Interaction via messages between two people. Only handles basic text for now.
 */

public class ChatActivity extends AppCompatActivity {

    public static final String ARG_NAME_OF_FRIEND = "NAME_OF_FRIEND";
    public static final String ARG_EMAIL_OF_FRIEND = "EMAIL_OF_FRIEND";
    public static final String ARG_CURRENT_THREAD_ID = "CURRENT_THREAD_ID";

    // Database references
    private DatabaseReference mStatusOfFriendReference;
    private DatabaseReference mTypingStateOfFriendReference;
    private DatabaseReference mTypingStateOfUserReference;
    private DatabaseReference mLastSeenOfFriendReference;
    private DatabaseReference mThreadMessagesReference;
    private DatabaseReference mFriendsThreadReference;
    private DatabaseReference mUsersThreadReference;

    // Listeners
    private ValueEventListener mStatusOfFriendListener;
    private ValueEventListener mTypingStateOfFriendListener;
    private ChildEventListener mThreadMessagesListener;

    // Views
    private ChatActivityBinding binding;
    private TextView mStatusTextView;
    private RecyclerView mRecyclerView;

    private ChatListAdapter mChatListAdapter;
    private UserModel mCurrentUser;
    private boolean mFriendsStatusSet;
    private boolean mIsTyping;

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
        initializeSendButtonBehaviour();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeStatusOfFriendListener();
        initializeThreadMessagesEventListener();
        mFriendsStatusSet = false;
        mIsTyping = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        setTypingState(false);

        if (mStatusOfFriendListener != null) {
            mStatusOfFriendReference.removeEventListener(mStatusOfFriendListener);
        }

        if (mTypingStateOfFriendListener != null) {
            mTypingStateOfFriendReference.removeEventListener(mTypingStateOfFriendListener);
        }

        if (mThreadMessagesListener != null) {
            mThreadMessagesReference.removeEventListener(mThreadMessagesListener);
        }
    }

    private void initializeDatabase(String emailOfFriend, String threadID) {
        DatabaseReference databaseReference
                = DatabaseUtil.getDatabase().getReference();

        DatabaseReference currentThreadReference
                = databaseReference.child("threads").child(threadID);

        String encodedEmailOfFriend
                = User.getEncodedEmail(emailOfFriend);

        mCurrentUser = User.getCurrentUserModel();

        if (mCurrentUser != null) {
            String currentUsersEncodedEmail = mCurrentUser.getEncodedEmail();

            // Friend reference initialization
            mTypingStateOfFriendReference
                    = currentThreadReference.child("members")
                    .child(encodedEmailOfFriend)
                    .child("is_typing");

            mStatusOfFriendReference
                    = databaseReference.child("users")
                    .child(encodedEmailOfFriend)
                    .child("connections");

            mLastSeenOfFriendReference
                    = databaseReference.child("users")
                    .child(encodedEmailOfFriend)
                    .child("lastSeen");

            mFriendsThreadReference = databaseReference.child("users")
                    .child(encodedEmailOfFriend)
                    .child("user_threads_with")
                    .child(currentUsersEncodedEmail);

            // Current user reference initialization
            mTypingStateOfUserReference
                    = currentThreadReference.child("members")
                    .child(currentUsersEncodedEmail)
                    .child("is_typing");

            mUsersThreadReference
                    = databaseReference.child("users")
                    .child(currentUsersEncodedEmail)
                    .child("user_threads_with")
                    .child(encodedEmailOfFriend);
        }

        // Thread messages reference initialization
        mThreadMessagesReference
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
            mStatusTextView = customActionBar.findViewById(R.id.statusTextView);

            nameTextView.setText(nameOfFriend);
        }
    }

    private void initializeRecyclerView(String threadID) {
        mRecyclerView = binding.chatRecyclerView;

        // Specify our adapter
        mChatListAdapter = new ChatListAdapter
                (this, MessageModel.class, MessageModel.timeComparator, threadID);

        // Add a callback to scroll to the bottom after adding a new message
        mChatListAdapter.addCallback(new SortedListAdapter.Callback() {
            @Override
            public void onEditStarted() {
                // Do nothing
            }

            @Override
            public void onEditFinished() {
                mRecyclerView.smoothScrollToPosition(0);
            }
        });
        mRecyclerView.setAdapter(mChatListAdapter);
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
        // Check if the current typing state is the same as the calling function's typing state,
        // to reduce wasted data usage.
        if (mIsTyping != isTyping) {
            mTypingStateOfUserReference.setValue(isTyping);
            mIsTyping = isTyping;
        }
    }

    private void initializeSendButtonBehaviour() {

        binding.chatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = binding.chatMessageArea.getText().toString();

                if (!message.isEmpty()) {
                    String email = mCurrentUser.getEmail();

                    // Outgoing message using this constructor sets the timestamp as a placeholder
                    // to be replaced with the server's timestamp once received; this is also
                    // performed locally even if offline, with the estimated timestamp.
                    MessageModel outgoingMessage = new MessageModel(email, message);

                    // We reduce the number of child-changed events by sending a single POJO, with
                    // the downside of increased complexity of juggling between an Object and Long
                    // type in the MessageModel
                    mThreadMessagesReference.push().setValue(outgoingMessage);

                    // Push the message and timestamp to both the user's thread nodes
                    mFriendsThreadReference.child("lastMessage").setValue(message);
                    mFriendsThreadReference.child("timestamp").setValue(ServerValue.TIMESTAMP);

                    mUsersThreadReference.child("lastMessage").setValue(message);
                    mUsersThreadReference.child("timestamp").setValue(ServerValue.TIMESTAMP);

                    binding.chatMessageArea.setText("");
                }
            }
        });
    }

    private void initializeStatusOfFriendListener() {
        if (mStatusOfFriendListener == null) {
            mStatusOfFriendListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists()) {
                        if (!mFriendsStatusSet) {
                            // Since a user can have multiple devices, we only set it to online for
                            // the first "online" status update. This is to prevent multiple listeners
                            // set for the typing activity.
                            mStatusTextView.setText(R.string.user_online);

                            // Initialize listener for their typing activity
                            initializeTypingStateOfFriendListener();

                            mFriendsStatusSet = true;
                        }
                    } else {
                        // Friend is offline, so we remove the typing activity listener
                        if (mTypingStateOfFriendListener != null) {
                            mTypingStateOfFriendReference
                                    .removeEventListener(mTypingStateOfFriendListener);
                        }

                        setLastSeenOfFriend();
                        mFriendsStatusSet = false;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w("Error", databaseError.toException().getMessage());
                }
            };
        }

        mStatusOfFriendReference.addValueEventListener(mStatusOfFriendListener);
    }

    private void initializeTypingStateOfFriendListener() {

        if (mTypingStateOfFriendListener == null) {
            mTypingStateOfFriendListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    Boolean isTyping = dataSnapshot.getValue(Boolean.class);

                    if (isTyping != null) {
                        if (isTyping) {
                            mStatusTextView.setText(R.string.is_typing);
                        } else {
                            // We assume that if a client had sent a "false" value for isTyping,
                            // and this listener was triggered, they'd still be online to do so.
                            mStatusTextView.setText(R.string.user_online);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w("Error", databaseError.toException().getMessage());
                }
            };
        }

        mTypingStateOfFriendReference.addValueEventListener(mTypingStateOfFriendListener);
    }

    private void setLastSeenOfFriend() {
        mLastSeenOfFriendReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Long lastSeen = dataSnapshot.getValue(Long.class);

                if (lastSeen != null) {
                    TimestampInterpreter interpreter = new TimestampInterpreter(lastSeen);
                    String interpretation = interpreter.getTimestampInterpretation();

                    switch (interpretation) {
                        case TimestampInterpreter.TODAY:
                            mStatusTextView
                                    .setText(String.format("Last seen today at %s",
                                            interpreter.getTime()));
                            break;
                        case TimestampInterpreter.YESTERDAY:
                            mStatusTextView
                                    .setText(String.format("Last seen yesterday at %s",
                                            interpreter.getTime()));
                            break;
                        default:
                            mStatusTextView
                                    .setText(String.format("Last seen: %s",
                                            interpreter.getFullDate()));
                            break;
                    }

                } else {
                    mStatusTextView.setText(R.string.user_inactive);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        });
    }

    private void initializeThreadMessagesEventListener() {
        if (mThreadMessagesListener == null) {
            mThreadMessagesListener = new ChildEventListener() {

                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    MessageModel incomingMessage = dataSnapshot.getValue(MessageModel.class);

                    if (incomingMessage != null) {
                        // Set the message ID to the push key
                        incomingMessage.messageID = dataSnapshot.getKey();
                        addMessageModel(incomingMessage);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    // There are currently two cases where a child can be changed: (1) A message
                    // has been seen, and (2) ServerValue.TIMESTAMP has been replaced with the
                    // server's current timestamp
                    MessageModel incomingMessage = dataSnapshot.getValue(MessageModel.class);

                    if (incomingMessage != null) {
                        incomingMessage.messageID = dataSnapshot.getKey();
                        addMessageModel(incomingMessage);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    // Removing messages is not currently supported
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    // Priority/ordering changes shouldn't happen
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w("Error", databaseError.toException().getMessage());
                }
            };
        }

        mThreadMessagesReference.addChildEventListener(mThreadMessagesListener);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void addMessageModel(MessageModel model) {
        mChatListAdapter.edit()
                .add(model)
                .commit();
    }
}
