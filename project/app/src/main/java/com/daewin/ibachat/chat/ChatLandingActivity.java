package com.daewin.ibachat.chat;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.ChatLandingActivityBinding;
import com.daewin.ibachat.friends.FindFriendActivity;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.user.UserModel;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chat Landing which displays the user's current chats. This would be the main activity after login.
 */

public class ChatLandingActivity extends AppCompatActivity {

    private static final String EXTRA_IDP_RESPONSE = "extra_idp_response";
    private static final int COUNTER_DELTA = 1;

    private ChatLandingActivityBinding binding;
    private DatabaseReference mDatabase;
    private DatabaseReference mRequestsReceivedReference;
    private String mCurrentUsersEncodedEmail;
    private AtomicInteger notificationCounter;
    private Menu mToolbarMenu;

    @NonNull
    public static Intent createIntent(Context context, IdpResponse idpResponse) {

        Intent startIntent = new Intent();
        if (idpResponse != null) {
            startIntent.putExtra(EXTRA_IDP_RESPONSE, idpResponse);
        }

        return startIntent.setClass(context, ChatLandingActivity.class);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.chat_landing_activity);
        setSupportActionBar(binding.myToolbar);

        // Default value is 0
        notificationCounter = new AtomicInteger();

        initializeDatabaseReferences();
        initializeNotifications();

    }

    private void initializeDatabaseReferences(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser != null) {
            UserModel currentUser = new UserModel(currentFirebaseUser.getDisplayName(),
                    currentFirebaseUser.getEmail());

            if (currentUser.exists()) {
                mCurrentUsersEncodedEmail = User.getEncodedEmail(currentUser.getEmail());

                // Set a listener for notifications (currently it's just friend requests)
                mRequestsReceivedReference = mDatabase.child("users")
                        .child(mCurrentUsersEncodedEmail)
                        .child("friend_requests_received");
            }
        }
    }

    // TODO: Clean up listener
    private void initializeNotifications(){
        mRequestsReceivedReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(notificationCounter.addAndGet(COUNTER_DELTA) == 1){
                    // First notification, so change the icon to an active notification
                    getNotificationMenuItem()
                            .setIcon(R.drawable.ic_notifications_active_white_24dp);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        });
    }

    private MenuItem getNotificationMenuItem(){
        return mToolbarMenu.findItem(R.id.action_notifications);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        this.mToolbarMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_add_friend:

                startActivity(new Intent(this, FindFriendActivity.class));
                return true;

            case R.id.action_settings:
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
