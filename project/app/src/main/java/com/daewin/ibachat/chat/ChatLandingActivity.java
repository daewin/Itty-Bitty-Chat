package com.daewin.ibachat.chat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.daewin.ibachat.MainActivity;
import com.daewin.ibachat.MyLifecycleObserver;
import com.daewin.ibachat.R;
import com.daewin.ibachat.database.DatabaseUtil;
import com.daewin.ibachat.databinding.ChatLandingActivityBinding;
import com.daewin.ibachat.friends.FindFriendActivity;
import com.daewin.ibachat.model.ThreadModel;
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.notification.NotificationActivity;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.user.UserPresence;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat Landing which displays the user's current chats. This would be the main activity after login.
 */

public class ChatLandingActivity extends AppCompatActivity {

    private static final String EXTRA_IDP_RESPONSE = "extra_idp_response";

    private DatabaseReference mRequestsReceivedReference;
    private DatabaseReference mUserThreadsReference;

    private ValueEventListener mNotificationsListener;
    private ValueEventListener mUserThreadsListener;

    private Menu mToolbarMenu;
    private RecyclerView mRecyclerView;
    private ChatLandingActivityBinding binding;

    private ChatLandingListAdapter chatLandingListAdapter;

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

        binding = DataBindingUtil.setContentView(this, R.layout.chat_landing_activity);

        setSupportActionBar(binding.myToolbar);

        binding.mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), StartNewChatActivity.class));
            }
        });

        initializeDatabaseReferences();
        initializeRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeUserThreads();
        initializeNotifications();
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeListeners();
    }

    private void initializeDatabaseReferences() {
        DatabaseReference mDatabase = DatabaseUtil.getDatabase().getReference();
        UserModel currentUser = User.getCurrentUserModel();

        if (currentUser != null) {
            String mCurrentUsersEncodedEmail = currentUser.getEncodedEmail();

            // Set a listener for notifications (currently it's just friend requests)
            mRequestsReceivedReference = mDatabase.child("users")
                    .child(mCurrentUsersEncodedEmail)
                    .child("friend_requests_received");

            mUserThreadsReference = mDatabase.child("users")
                    .child(mCurrentUsersEncodedEmail)
                    .child("user_threads_with");
        }
    }

    private void initializeRecyclerView() {
        mRecyclerView = binding.mRecyclerView;

        // Specify our adapter
        chatLandingListAdapter = new ChatLandingListAdapter
                (this, ThreadModel.class, ThreadModel.timeComparator);

        mRecyclerView.setAdapter(chatLandingListAdapter);
        mRecyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addItemDecoration
                (new DividerItemDecoration(mRecyclerView.getContext(),
                        mLayoutManager.getOrientation()));
    }


    private void initializeUserThreads() {

        binding.chatLandingProgressBar.setVisibility(View.VISIBLE);

        mUserThreadsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<ThreadModel> threadModels = new ArrayList<>();

                for (DataSnapshot threads : dataSnapshot.getChildren()) {
                    ThreadModel thread = threads.getValue(ThreadModel.class);

                    if (thread != null && thread.getTimestamp() != null) {
                        // We only get threads that have any chat activity
                        thread.setEmail(threads.getKey());
                        threadModels.add(thread);
                    }
                }

                binding.chatLandingProgressBar.setVisibility(View.INVISIBLE);

                if (threadModels.size() == 0) {
                    binding.noChatsTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.noChatsTextView.setVisibility(View.INVISIBLE);
                }

                updateAdapterList(threadModels);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        };

        mUserThreadsReference.addValueEventListener(mUserThreadsListener);
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

    private void setNotificationIcon(int drawable) {
        // Check if the current notification icon has already been set, else set it.
        MenuItem menuItem = getNotificationMenuItem();

        if (menuItem != null) {
            Drawable potentialIcon = ContextCompat.getDrawable(getApplicationContext(), drawable);

            if (!menuItem.getIcon().equals(potentialIcon)) {
                getNotificationMenuItem().setIcon(drawable);
            }
        }
    }

    @Nullable
    private MenuItem getNotificationMenuItem() {
        if (mToolbarMenu != null) {
            return mToolbarMenu.findItem(R.id.action_notifications);
        } else {
            return null;
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

            case R.id.action_logout:
                showLogoutConfirmationDialog();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        logout();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void logout() {
        UserPresence.getInstance().forceRemoveCurrentConnection();
        UserPresence.clearInstance();
        removeListeners();

        final Intent logoutIntent
                = new Intent(ChatLandingActivity.this, MainActivity.class);

        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {

                    public void onComplete(@NonNull Task<Void> task) {
                        // The user is now signed out
                        startActivity(logoutIntent);
                        finishAffinity();
                    }
                });
    }

    private void removeListeners(){
        if (mNotificationsListener != null) {
            mRequestsReceivedReference.removeEventListener(mNotificationsListener);
        }

        if (mUserThreadsListener != null) {
            mUserThreadsReference.removeEventListener(mUserThreadsListener);
        }
    }

    private void updateAdapterList(List<ThreadModel> threadModels) {

        chatLandingListAdapter.edit()
                .replaceAll(threadModels)
                .commit();

        mRecyclerView.scrollToPosition(0);
    }
}
