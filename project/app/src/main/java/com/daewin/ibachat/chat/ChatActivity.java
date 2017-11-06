package com.daewin.ibachat.chat;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 */

public class ChatActivity extends AppCompatActivity {

    public static final String ARG_NAME_OF_FRIEND = "NAME_OF_FRIEND";
    public static final String ARG_EMAIL_OF_FRIEND = "EMAIL_OF_FRIEND";
    public static final String ARG_CURRENT_THREAD_ID = "CURRENT_THREAD_ID";

    // Database references
    private DatabaseReference statusOfFriendReference;
    private DatabaseReference typingStateOfFriendReference;
    private DatabaseReference lastSeenOfFriendReference;
    private DatabaseReference typingStateOfUserReference;

    // Listeners
    private ValueEventListener statusOfFriendListener;
    private ValueEventListener typingStateOfFriendListener;

    private ChatActivityBinding binding;
    private TextView statusTextView;
    private boolean friendsStatusSet;
    private String nameOfFriend;
    private String emailOfFriend;
    private String threadID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new MyLifecycleObserver());

        binding = DataBindingUtil.setContentView(this, R.layout.chat_activity);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        nameOfFriend = getIntent().getStringExtra(ARG_NAME_OF_FRIEND);
        emailOfFriend = getIntent().getStringExtra(ARG_EMAIL_OF_FRIEND);
        threadID = getIntent().getStringExtra(ARG_CURRENT_THREAD_ID);

        initializeDatabase();
        initializeActionBar();
        initializeTypingState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeStatusListenerForFriend();

        friendsStatusSet = false;
    }

    @Override
    protected void onPause() {
        setTypingState(false);
        super.onPause();
    }

    @Override
    protected void onStop() {

        if (statusOfFriendListener != null) {
            statusOfFriendReference.removeEventListener(statusOfFriendListener);
        }

        if (typingStateOfFriendListener != null) {
            typingStateOfFriendReference.removeEventListener(typingStateOfFriendListener);
        }

        super.onStop();
    }

    private void initializeDatabase() {

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

        statusOfFriendReference
                = databaseReference.child("users")
                .child(encodedEmailOfFriend)
                .child("connections");

        lastSeenOfFriendReference
                = databaseReference.child("users")
                .child(encodedEmailOfFriend)
                .child("lastSeen");


        // Current user reference initialization
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser != null) {
            UserModel currentUser = new UserModel(currentFirebaseUser.getDisplayName(),
                    currentFirebaseUser.getEmail());

            if (currentUser.exists()) {
                String mCurrentUsersEncodedEmail = User.getEncodedEmail(currentUser.getEmail());

                typingStateOfUserReference
                        = currentThreadReference.child("members")
                        .child(mCurrentUsersEncodedEmail)
                        .child("is_typing");
            }
        }
    }

    private void initializeActionBar() {
        // Custom Action Bar
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

    private void initializeStatusListenerForFriend() {

        statusOfFriendListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    if(!friendsStatusSet){
                        // Since a user can have multiple devices, we only set it to online for
                        // the first "online" status update. This is to prevent multiple listeners
                        // set for the typing activity.
                        statusTextView.setText(R.string.user_online);

                        // Initialize listener for their typing activity
                        initializeTypingListenerForFriend();

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


    private void initializeTypingListenerForFriend() {

        typingStateOfFriendListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Boolean isTyping = dataSnapshot.getValue(Boolean.class);

                if (isTyping != null) {
                    if (isTyping) {
                        statusTextView.setText(R.string.is_typing);
                    } else {
                        // We assume that if a client had sent a "false" value for isTyping,
                        // and the listener was triggered, they'd still be online to do so.
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
                    statusTextView.setText("Last seen: " + lastSeen);
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
