package com.daewin.ibachat.notification;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.daewin.ibachat.MyLifecycleObserver;
import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.NotificationActivityBinding;
import com.daewin.ibachat.model.UserRequestModel;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Notifications for the user. Currently only shows friend requests, sorted according to the time of
 * request
 */

public class NotificationActivity extends AppCompatActivity {

    private NotificationActivityBinding binding;
    private DatabaseReference mRequestsReceivedReference;
    private RecyclerView mRecyclerView;
    private NotificationListAdapter mNotificationListAdapter;
    private ValueEventListener mNotificationsListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new MyLifecycleObserver());

        binding = DataBindingUtil.setContentView(this, R.layout.notification_activity);

        // Toolbar settings
        setSupportActionBar(binding.notificationToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        initializeDatabaseReferences();
        initializeRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeDetailedNotifications();
    }

    @Override
    protected void onStop() {
        if(mNotificationsListener != null){
            mRequestsReceivedReference.removeEventListener(mNotificationsListener);
        }
        super.onStop();
    }

    private void initializeDatabaseReferences() {
        DatabaseReference userDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser != null) {
            UserModel currentUser = new UserModel(currentFirebaseUser.getDisplayName(),
                    currentFirebaseUser.getEmail());

            if (currentUser.exists()) {
                String currentUsersEncodedEmail = User.getEncodedEmail(currentUser.getEmail());

                mRequestsReceivedReference = userDatabase
                        .child(currentUsersEncodedEmail)
                        .child("friend_requests_received");
            }
        }
    }

    private void initializeRecyclerView() {
        mRecyclerView = binding.notificationRecyclerView;

        mRecyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Specify our adapter
        mNotificationListAdapter = new NotificationListAdapter
                (this, UserRequestModel.class, UserRequestModel.timeComparator);

        mRecyclerView.setAdapter(mNotificationListAdapter);
    }

    private void initializeDetailedNotifications() {
        mNotificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                List<UserRequestModel> userRequestModels = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    String decodedEmail = User.getDecodedEmail(snapshot.getKey());
                    String name = snapshot.child("name").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    if (timestamp != null){
                        UserRequestModel userRequestModel
                                = new UserRequestModel(name, decodedEmail, timestamp);

                        if(userRequestModel.exists()){
                            userRequestModels.add(userRequestModel);
                        }
                    }
                }

                if(userRequestModels.isEmpty()){
                    updateAdapterList(userRequestModels);
                    binding.noNotificationsTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.noNotificationsTextView.setVisibility(View.INVISIBLE);
                    updateAdapterList(userRequestModels);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        };

        mRequestsReceivedReference.addValueEventListener(mNotificationsListener);
    }

    private void updateAdapterList(List<UserRequestModel> userRequestModels) {

        mNotificationListAdapter.edit()
                .replaceAll(userRequestModels)
                .commit();

        mRecyclerView.scrollToPosition(0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
