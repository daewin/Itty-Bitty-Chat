package com.daewin.ibachat.chat;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daewin.ibachat.databinding.ChatLandingItemBinding;
import com.daewin.ibachat.model.ThreadModel;
import com.daewin.ibachat.timestamp.TimestampInterpreter;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Comparator;

/**
 */

public class ChatLandingListAdapter extends SortedListAdapter<ThreadModel> {

    private DatabaseReference mDatabase;

    ChatLandingListAdapter(@NonNull Context context,
                           @NonNull Class<ThreadModel> aClass,
                           @NonNull Comparator<ThreadModel> comparator) {
        super(context, aClass, comparator);
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    protected ViewHolder<? extends ThreadModel> onCreateViewHolder
            (@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup, int i) {

        ChatLandingItemBinding binding
                = ChatLandingItemBinding.inflate(layoutInflater, viewGroup, false);

        return new ChatLandingViewHolder(binding);
    }


    private class ChatLandingViewHolder extends SortedListAdapter.ViewHolder<ThreadModel> {

        private ChatLandingItemBinding binding;

        ChatLandingViewHolder(@NonNull ChatLandingItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        protected void performBind(@NonNull final ThreadModel threadModel) {

            binding.setThread(threadModel);

            TimestampInterpreter interpreter = new TimestampInterpreter(threadModel.getTimestamp());
            String interpretation = interpreter.getTimestampInterpretation();
            String time;

            switch (interpretation) {
                case TimestampInterpreter.TODAY:
                    time = interpreter.getTime();
                    break;
                case TimestampInterpreter.YESTERDAY:
                    time = "Yesterday at " + interpreter.getTime();
                    break;
                default:
                    time = interpreter.getFullDate();
                    break;
            }

            binding.timeText.setText(time);

            binding.threadConstraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent startChat = new Intent(view.getContext(), ChatActivity.class);
                    startChat.putExtra(ChatActivity.ARG_NAME_OF_FRIEND, threadModel.getName());
                    startChat.putExtra(ChatActivity.ARG_EMAIL_OF_FRIEND, threadModel.getEmail());
                    startChat.putExtra(ChatActivity.ARG_CURRENT_THREAD_ID, threadModel.getId());
                    view.getContext().startActivity(startChat);
                }
            });

        }
    }
}
