package com.daewin.ibachat.notification;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.daewin.ibachat.databinding.NotificationListItemBinding;
import com.daewin.ibachat.user.UserModel;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;

import java.util.Comparator;

/**
 * Recycler View Adapter for displaying a list of notifications. Currently only displays
 * friend requests but can be expanded in the future to include other types.
 * <p>
 * SortedListAdapter obtained from: https://wrdlbrnft.github.io/SortedListAdapter/
 */

public class NotificationListAdapter extends SortedListAdapter<UserRequestModel> {


    public NotificationListAdapter(@NonNull Context context,
                                   @NonNull Class<UserRequestModel> aClass,
                                   @NonNull Comparator<UserRequestModel> comparator) {
        super(context, aClass, comparator);
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
        }
    }
}
