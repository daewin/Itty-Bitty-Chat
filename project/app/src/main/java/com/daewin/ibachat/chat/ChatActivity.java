package com.daewin.ibachat.chat;

import android.app.Application;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
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

    private ChatActivityBinding binding;
    private String nameOfFriend;
    private String emailOfFriend;
    private String threadID;
    private DatabaseReference typingStateOfFriendReference;
    private ValueEventListener typingStateOfFriendListener;
    private DatabaseReference typingStateOfUserReference;
    private String mCurrentUsersEncodedEmail;
    private String mCurrentUsersName;
    private DatabaseReference statusOfFriendReference;
    private ValueEventListener statusOfFriendListener;
    private DatabaseReference lastSeenOfFriendReference;
    private ValueEventListener lastSeenOfFriendListener;

    private boolean isFriendOnline = false;
    private boolean isFriendTyping = false;

    private TextView statusTextView;

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
    protected void onPause() {
        setTypingState(false);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeStatusListenerForFriend();
        initializeTypingListenerForFriend();
    }

    @Override
    protected void onStop() {
        if (typingStateOfFriendListener != null) {
            typingStateOfFriendReference.removeEventListener(typingStateOfFriendListener);
            typingStateOfFriendListener = null;
        }

        if (statusOfFriendListener != null) {
            statusOfFriendReference.removeEventListener(statusOfFriendListener);
            statusOfFriendListener = null;
        }

        if(lastSeenOfFriendListener != null){
            lastSeenOfFriendReference.removeEventListener(lastSeenOfFriendListener);
            lastSeenOfFriendListener = null;
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
                mCurrentUsersEncodedEmail = User.getEncodedEmail(currentUser.getEmail());
                mCurrentUsersName = currentUser.getName();

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
                    isFriendOnline = true;
                    coordinateStatusUpdates("Online");

                } else {
                    isFriendOnline = false;
                    coordinateStatusUpdates("Offline");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        statusOfFriendReference.addValueEventListener(statusOfFriendListener);
    }


    private void initializeTypingListenerForFriend() {

        typingStateOfFriendListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Boolean isTyping = dataSnapshot.getValue(Boolean.class);

                if(isTyping != null){
                    if (isTyping) {
                        isFriendTyping = true;
                        coordinateStatusUpdates("Typing");
                    } else {
                        isFriendTyping = false;
                        coordinateStatusUpdates("Not Typing");
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        typingStateOfFriendReference.addValueEventListener(typingStateOfFriendListener);
    }


    private void initializeLastSeenOfFriendListener() {

        lastSeenOfFriendListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Long lastSeen = dataSnapshot.getValue(Long.class);

                if(lastSeen != null) {
                    statusTextView.setText("Last seen: " + lastSeen);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        lastSeenOfFriendReference.addValueEventListener(lastSeenOfFriendListener);
    }

    private void coordinateStatusUpdates(String state){

        if(state.equals("Online") || state.equals("Typing")){

            if(state.equals("Typing")){
                // Online (implicit) and typing
                statusTextView.setText(R.string.is_typing);
            } else {
                // Online but not typing
                statusTextView.setText(R.string.user_online);
            }
        } else if (state.equals("Offline")){
            // Offline
            initializeLastSeenOfFriendListener();
            statusTextView.setText("Last seen: ");

        }

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
