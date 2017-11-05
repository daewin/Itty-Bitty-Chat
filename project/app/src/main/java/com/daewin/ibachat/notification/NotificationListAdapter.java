package com.daewin.ibachat.notification;

import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daewin.ibachat.databinding.NotificationListItemBinding;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.user.UserModel;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Comparator;

/**
 * Recycler View Adapter for displaying a list of notifications. Currently only displays
 * friend requests but can be expanded in the future to include other types.
 * <p>
 * SortedListAdapter obtained from: https://wrdlbrnft.github.io/SortedListAdapter/
 */

public class NotificationListAdapter extends SortedListAdapter<UserRequestModel> {
    // TODO: Add timedate
    private DatabaseReference mUserDatabase;
    private String mCurrentUsersEncodedEmail;
    private String mCurrentUsersName;

    NotificationListAdapter(@NonNull Context context,
                                   @NonNull Class<UserRequestModel> aClass,
                                   @NonNull Comparator<UserRequestModel> comparator) {
        super(context, aClass, comparator);
        initializeDatabaseReferences();
    }


    private void initializeDatabaseReferences() {
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser != null) {
            UserModel currentUser = new UserModel(currentFirebaseUser.getDisplayName(),
                    currentFirebaseUser.getEmail());

            if (currentUser.exists()) {
                mCurrentUsersEncodedEmail = User.getEncodedEmail(currentUser.getEmail());
                mCurrentUsersName = currentUser.getName();
            }
        }
    }

    @NonNull
    @Override
    protected ViewHolder<? extends UserRequestModel> onCreateViewHolder
            (@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup, int i) {

        NotificationListItemBinding binding
                = NotificationListItemBinding.inflate(layoutInflater, viewGroup, false);

        return new NotificationViewHolder(binding);
    }

    private class NotificationViewHolder extends SortedListAdapter.ViewHolder<UserRequestModel> {

        private NotificationListItemBinding binding;

        NotificationViewHolder(@NonNull NotificationListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        protected void performBind(@NonNull UserRequestModel userRequestModel) {
            binding.setUser(userRequestModel);
            binding.executePendingBindings();

            final String friendsName = userRequestModel.getName();
            final String encodedFriendsEmail = User.getEncodedEmail(userRequestModel.getEmail());

            binding.acceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    acceptRequest(friendsName, encodedFriendsEmail);
                }
            });

            binding.declineButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    removeRequests(mCurrentUsersEncodedEmail, encodedFriendsEmail);
                }
            });
        }

        private void acceptRequest(String friendsName, String encodedFriendsEmail){
            // Add the requesting user to our current user's friends list
            mUserDatabase.child(mCurrentUsersEncodedEmail)
                    .child("friends")
                    .child(encodedFriendsEmail)
                    .setValue(friendsName);

            // Add our current user to the requesting user's friends list
            mUserDatabase.child(encodedFriendsEmail)
                    .child("friends")
                    .child(mCurrentUsersEncodedEmail)
                    .setValue(mCurrentUsersName);

            removeRequests(mCurrentUsersEncodedEmail, encodedFriendsEmail);
        }

        private void removeRequests(String currentUsername, String requestingUsername){
            // Remove the friend_requests_received entry from our currentUser
            mUserDatabase.child(currentUsername)
                    .child("friend_requests_received")
                    .child(requestingUsername)
                    .removeValue();

            // Remove the friend_requests_sent entry from the requesting user
            mUserDatabase.child(requestingUsername)
                    .child("friend_requests_sent")
                    .child(currentUsername)
                    .removeValue();

            // In the case that our currentUser also sent a friend request, let's try to remove it.
            // This helps solve two-way requests and any issues further down the line.

            // Remove the friend_requests_received entry from the requesting user
            mUserDatabase.child(requestingUsername)
                    .child("friend_requests_received")
                    .child(currentUsername)
                    .removeValue();

            // Remove the friend_requests_sent entry from our currentUser
            mUserDatabase.child(currentUsername)
                    .child("friend_requests_sent")
                    .child(requestingUsername)
                    .removeValue();

        }
    }
}
