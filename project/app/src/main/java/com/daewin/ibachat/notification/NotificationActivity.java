package com.daewin.ibachat.notification;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.NotificationActivityBinding;
import com.daewin.ibachat.friends.FindFriendListAdapter;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.user.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Comparator;

/**
 * Notifications for the user. Currently only shows friend requests, sorted according to the time of
 * request
 */

public class NotificationActivity extends AppCompatActivity {

    private NotificationActivityBinding binding;
    private DatabaseReference mUserDatabase;
    private DatabaseReference mRequestsReceivedReference;
    private String mCurrentUsersEncodedEmail;
    private RecyclerView mRecyclerView;
    private NotificationListAdapter mNotificationListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.notification_activity);

        // Toolbar settings
        setSupportActionBar(binding.notificationToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        initializeDatabaseReferences();

        mRecyclerView = binding.notificationRecyclerView;

        initializeRecyclerView();


    }

    private void initializeDatabaseReferences(){
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser != null) {
            UserModel currentUser = new UserModel(currentFirebaseUser.getDisplayName(),
                    currentFirebaseUser.getEmail());

            if (currentUser.exists()) {
                mCurrentUsersEncodedEmail = User.getEncodedEmail(currentUser.getEmail());

                mRequestsReceivedReference = mUserDatabase.child(mCurrentUsersEncodedEmail)
                        .child("friend_requests_received");
            }
        }
    }

    private void initializeRecyclerView() {
        // Use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Specify our adapter
        mNotificationListAdapter = new NotificationListAdapter
                (this, UserRequestModel.class, UserRequestModel.timeComparator);

        mRecyclerView.setAdapter(mNotificationListAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
