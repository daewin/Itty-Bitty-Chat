package com.daewin.ibachat.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daewin.ibachat.databinding.StartNewChatItemBinding;
import com.daewin.ibachat.user.UserModel;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;

import java.util.Comparator;

/**
 */

public class StartNewChatListAdapter extends SortedListAdapter<UserModel> {

    public StartNewChatListAdapter(@NonNull Context context,
                                   @NonNull Class<UserModel> aClass,
                                   @NonNull Comparator<UserModel> comparator) {
        super(context, aClass, comparator);
    }

    @NonNull
    @Override
    protected ViewHolder<? extends UserModel> onCreateViewHolder
            (@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup, int i) {

        StartNewChatItemBinding binding =
                StartNewChatItemBinding.inflate(layoutInflater, viewGroup, false);

        return new FriendViewHolder(binding);
    }

    private class FriendViewHolder extends SortedListAdapter.ViewHolder<UserModel> {

        private StartNewChatItemBinding binding;

        FriendViewHolder(@NonNull StartNewChatItemBinding binding) {
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
