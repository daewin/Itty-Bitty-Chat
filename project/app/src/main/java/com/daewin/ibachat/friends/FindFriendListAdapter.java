package com.daewin.ibachat.friends;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daewin.ibachat.database.DatabaseUtil;
import com.daewin.ibachat.databinding.FindFriendListItemBinding;
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.user.User;
import com.github.wrdlbrnft.modularadapter.ModularAdapter;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Recycler View Adapter for displaying a list of people to add. Currently displays all the
 * users in the database, while being searchable. In the future, we could implement a suggested
 * friends list based on mutual friends, location, proximity etc.
 * <p>
 * SortedListAdapter obtained from: https://wrdlbrnft.github.io/SortedListAdapter/
 */


public class FindFriendListAdapter extends SortedListAdapter<UserModel> {

    private DatabaseReference mFriendsReference;
    private DatabaseReference mRequestsReference;
    private DatabaseReference mDatabase;
    private String mCurrentUsersEncodedEmail;
    private String mCurrentUsersName;

    FindFriendListAdapter(@NonNull Context context,
                          @NonNull Class<UserModel> aClass,
                          @NonNull Comparator<UserModel> comparator) {
        super(context, aClass, comparator);
        initializeDatabaseReferences();
    }

    private void initializeDatabaseReferences() {
        mDatabase = DatabaseUtil.getDatabase().getReference();
        UserModel currentUser = User.getCurrentUserModel();

        if (currentUser != null) {
            mCurrentUsersEncodedEmail = currentUser.getEncodedEmail();
            mCurrentUsersName = currentUser.getName();

            mFriendsReference = mDatabase.child("users")
                    .child(mCurrentUsersEncodedEmail)
                    .child("friends")
                    .getRef();

            mRequestsReference = mDatabase.child("users")
                    .child(mCurrentUsersEncodedEmail)
                    .child("friend_requests_sent")
                    .getRef();

        }
    }

    @NonNull
    @Override
    protected ViewHolder<? extends UserModel> onCreateViewHolder
            (@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup, int i) {

        FindFriendListItemBinding binding
                = FindFriendListItemBinding.inflate(layoutInflater, viewGroup, false);

        return new FriendViewHolder(binding);
    }

    @Override
    public void onViewRecycled(ModularAdapter.ViewHolder<? extends UserModel> holder) {
        FriendViewHolder friendViewHolder = (FriendViewHolder) holder;
        ValueEventListener friendsEventListener = friendViewHolder.friendsEventListener;

        if (friendsEventListener != null) {
            mFriendsReference.child(friendViewHolder.encodedFriendsEmail)
                    .removeEventListener(friendsEventListener);
        }

        super.onViewRecycled(holder);
    }

    private class FriendViewHolder extends SortedListAdapter.ViewHolder<UserModel> {

        private FindFriendListItemBinding binding;
        ValueEventListener friendsEventListener;
        String encodedFriendsEmail;

        FriendViewHolder(@NonNull FindFriendListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        protected void performBind(@NonNull UserModel userModel) {
            binding.setUser(userModel);
            binding.executePendingBindings();

            encodedFriendsEmail = User.getEncodedEmail(userModel.getEmail());

            // First we check if this "friend" is currently our friend, and we'll display the
            // icons accordingly. It's a ValueEventListener so that if they accept or decline,
            // we'll get updated and there won't be any database inconsistencies.
            friendsEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists()) {
                        // As both users are already friends, let's not show these options
                        binding.sendRequestButton.setVisibility(View.INVISIBLE);
                        binding.removeRequestButton.setVisibility(View.INVISIBLE);
                    } else {
                        checkRequestsAndDisplayButton(encodedFriendsEmail);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w("Error", databaseError.toException().getMessage());
                }
            };

            mFriendsReference.child(encodedFriendsEmail).addValueEventListener(friendsEventListener);


            binding.sendRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    sendRequest(encodedFriendsEmail);

                    binding.sendRequestButton.setVisibility(View.INVISIBLE);
                    binding.removeRequestButton.setVisibility(View.VISIBLE);
                }
            });

            binding.removeRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    removeRequest(encodedFriendsEmail);

                    binding.removeRequestButton.setVisibility(View.INVISIBLE);
                    binding.sendRequestButton.setVisibility(View.VISIBLE);
                }
            });
        }

        private void checkRequestsAndDisplayButton(final String encodedFriendsEmail) {
            // Get the friend requests sent list from our current user to decide
            // which button to display.
            mRequestsReference.child(encodedFriendsEmail)
                    .addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists()) {
                                // If the current user has already sent a request, show the
                                // remove request button
                                binding.sendRequestButton.setVisibility(View.INVISIBLE);
                                binding.removeRequestButton.setVisibility(View.VISIBLE);

                            } else {
                                binding.removeRequestButton.setVisibility(View.INVISIBLE);
                                binding.sendRequestButton.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.w("Error", databaseError.toException().getMessage());
                        }
                    });
        }

        private void sendRequest(String encodedFriendsEmail) {
            // Add an entry to the user's friend-requests-sent list in the database
            mDatabase.child("users")
                    .child(mCurrentUsersEncodedEmail)
                    .child("friend_requests_sent")
                    .child(encodedFriendsEmail)
                    .setValue(true);

            // Add a detailed entry to the friend's friend-requests-received list in the database
            Map<String, Object> value = new HashMap<>();
            value.put("name", mCurrentUsersName);
            value.put("timestamp", ServerValue.TIMESTAMP);

            mDatabase.child("users")
                    .child(encodedFriendsEmail)
                    .child("friend_requests_received")
                    .child(mCurrentUsersEncodedEmail)
                    .setValue(value);
        }


        private void removeRequest(String encodedFriendsEmail) {
            // Remove the friend request entry from the requesting user
            mDatabase.child("users")
                    .child(encodedFriendsEmail)
                    .child("friend_requests_received")
                    .child(mCurrentUsersEncodedEmail)
                    .removeValue();

            // Remove the friend request entry from our currentUser
            mDatabase.child("users")
                    .child(mCurrentUsersEncodedEmail)
                    .child("friend_requests_sent")
                    .child(encodedFriendsEmail)
                    .removeValue();
        }
    }
}
