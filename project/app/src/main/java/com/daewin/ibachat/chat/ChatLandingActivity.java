package com.daewin.ibachat.chat;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.daewin.ibachat.MyLifecycleObserver;
import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.ChatLandingActivityBinding;
import com.daewin.ibachat.friends.FindFriendActivity;
import com.daewin.ibachat.notification.NotificationActivity;
import com.daewin.ibachat.settings.SettingsActivity;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.user.UserPresence;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Chat Landing which displays the user's current chats. This would be the main activity after login.
 */

public class ChatLandingActivity extends AppCompatActivity {

    private static final String EXTRA_IDP_RESPONSE = "extra_idp_response";

    private DatabaseReference mRequestsReceivedReference;
    private Menu mToolbarMenu;
    private ValueEventListener mNotificationsListener;

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
        getLifecycle().addObserver(new MyLifecycleObserver());

        ChatLandingActivityBinding binding
                = DataBindingUtil.setContentView(this, R.layout.chat_landing_activity);

        setSupportActionBar(binding.myToolbar);

        initializeDatabaseReferences();

        binding.mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), StartNewChatActivity.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeNotifications();
    }

    @Override
    protected void onStop() {
        if (mNotificationsListener != null) {
            mRequestsReceivedReference.removeEventListener(mNotificationsListener);
        }

        super.onStop();
    }

    private void initializeDatabaseReferences() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser != null) {
            UserModel currentUser = new UserModel(currentFirebaseUser.getDisplayName(),
                    currentFirebaseUser.getEmail());

            if (currentUser.exists()) {
                String mCurrentUsersEncodedEmail = User.getEncodedEmail(currentUser.getEmail());

                // Set a listener for notifications (currently it's just friend requests)
                mRequestsReceivedReference = mDatabase.child("users")
                        .child(mCurrentUsersEncodedEmail)
                        .child("friend_requests_received");
            }
        }
    }

    private void initializeNotifications() {
        mNotificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Change the icon to an active notification icon
                    setNotificationIcon(R.drawable.ic_notifications_active_white_24dp);
                } else {
                    // No notifications left, so change the icon to an "empty" notification icon
                    setNotificationIcon(R.drawable.ic_notifications_none_white_24dp);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        };

        mRequestsReceivedReference.addValueEventListener(mNotificationsListener);
    }

    private MenuItem getNotificationMenuItem() {
        return mToolbarMenu.findItem(R.id.action_notifications);
    }

    private void setNotificationIcon(int drawable) {
        // Check if the current notification icon has already been set, else set it.
        if (!(getNotificationMenuItem().getIcon()
                .equals(ContextCompat.getDrawable(getApplicationContext(), drawable)))) {

            getNotificationMenuItem().setIcon(drawable);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        this.mToolbarMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_friend:

                startActivity(new Intent(this, FindFriendActivity.class));
                return true;

            case R.id.action_notifications:

                startActivity(new Intent(this, NotificationActivity.class));
                return true;

            case R.id.action_settings:

                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
