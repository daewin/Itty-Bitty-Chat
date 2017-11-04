package com.daewin.ibachat.friends;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.FindFriendListItemBinding;
import com.daewin.ibachat.user.UserModel;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;

import java.util.Comparator;

/**
 * Recycler View Adapter for displaying a list of people to add. Currently displays all the
 * users in the database, while being searchable. In the future, we could implement a suggested
 * friends list based on mutual friends, location, proximity etc.
 *
 * SortedListAdapter obtained from: https://wrdlbrnft.github.io/SortedListAdapter/
 */


public class FindFriendListAdapter extends SortedListAdapter<UserModel> {

    public FindFriendListAdapter(@NonNull Context context,
                                 @NonNull Class<UserModel> aClass,
                                 @NonNull Comparator<UserModel> comparator) {
        super(context, aClass, comparator);
    }

    @NonNull
    @Override
    protected ViewHolder<? extends UserModel> onCreateViewHolder
            (@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup, int i) {

        FindFriendListItemBinding binding
                = FindFriendListItemBinding.inflate(layoutInflater, viewGroup, false);

        return new FriendViewHolder(binding);
    }


    private class FriendViewHolder extends SortedListAdapter.ViewHolder<UserModel> {

        private final FindFriendListItemBinding binding;

        FriendViewHolder(@NonNull FindFriendListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        protected void performBind(@NonNull UserModel userModel) {
            binding.setUser(userModel);
            binding.executePendingBindings();
        }
    }
}
