package com.daewin.ibachat.chat;

import android.content.Context;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daewin.ibachat.database.DatabaseUtil;
import com.daewin.ibachat.databinding.ChatReceivedItemBinding;
import com.daewin.ibachat.databinding.ChatSentItemBinding;
import com.daewin.ibachat.model.MessageModel;
import com.daewin.ibachat.timestamp.TimestampInterpreter;
import com.daewin.ibachat.user.User;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.firebase.database.DatabaseReference;

import java.util.Comparator;

/**
 * Sorted List Adapter for displaying a chat list
 */

public class ChatListAdapter extends SortedListAdapter<MessageModel> {

    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 0;
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;

    private String currentUsersEmail;
    private DatabaseReference threadMessagesReference;

    ChatListAdapter(@NonNull Context context, @NonNull Class<MessageModel> aClass,
                    @NonNull Comparator<MessageModel> comparator, String threadID) {

        super(context, aClass, comparator);

        DatabaseReference databaseReference
                = DatabaseUtil.getDatabase().getReference();

        threadMessagesReference
                = databaseReference.child("thread_messages").child(threadID);

        String currentUsersEmail = User.getCurrentUsersEmail();
        if (currentUsersEmail != null) {
            this.currentUsersEmail = currentUsersEmail;
        }
    }

    @NonNull
    @Override
    protected ViewHolder<? extends MessageModel> onCreateViewHolder
            (@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup parent, int viewType) {

        if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            ChatReceivedItemBinding binding = ChatReceivedItemBinding.inflate
                    (LayoutInflater.from(parent.getContext()), parent, false);

            return new MessageViewHolder(binding, viewType);

        } else {
            ChatSentItemBinding binding = ChatSentItemBinding.inflate
                    (LayoutInflater.from(parent.getContext()), parent, false);

            return new MessageViewHolder(binding, viewType);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel messageModel = getItem(position);

        if (messageModel.email.equals(currentUsersEmail)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }


    class MessageViewHolder extends ViewHolder<MessageModel> {

        private static final String MESSAGE_SENT = "SENT";
        private static final String MESSAGE_SEEN = "SEEN";

        private ViewDataBinding binding;
        private int viewType;

        MessageViewHolder(ViewDataBinding binding, int viewType) {
            super(binding.getRoot());

            this.binding = binding;
            this.viewType = viewType;
        }

        @Override
        protected void performBind(@NonNull MessageModel messageModel) {
            if (messageModel.isTimestampLive()) {
                TimestampInterpreter interpreter
                        = new TimestampInterpreter(messageModel.getLiveTimestamp());

                String interpretation = interpreter.getTimestampInterpretation();
                String time;

                switch (interpretation) {
                    case TimestampInterpreter.TODAY:
                        time = "today at " + interpreter.getTime();
                        break;

                    case TimestampInterpreter.YESTERDAY:
                        time = "yesterday at " + interpreter.getTime();
                        break;

                    default:
                        time = interpreter.getFullDate();
                        break;
                }

                setIndividualItemBinding(messageModel, time);
            }
        }

        void setIndividualItemBinding(MessageModel messageModel, String time) {
            if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                bindMessageReceived(messageModel, time);

            } else {
                bindMessageSent(messageModel, time);
            }
        }

        private void bindMessageReceived(MessageModel messageModel, String time) {
            String message = messageModel.message;

            ChatReceivedItemBinding chatReceivedItemBinding = (ChatReceivedItemBinding) binding;
            chatReceivedItemBinding.setMessage(message);
            chatReceivedItemBinding.setTime(time);
            chatReceivedItemBinding.executePendingBindings();

            if (!chatReceivedItemBinding.messageTextView.isInLayout()) {
                chatReceivedItemBinding.messageTextView.requestLayout();
            }

            if (!messageModel.seen) {
                String messageID = messageModel.messageID;
                if (messageID != null && !messageID.isEmpty()) {
                    threadMessagesReference.child(messageID).child("seen").setValue(true);
                }
            }
        }

        private void bindMessageSent(MessageModel messageModel, String time) {
            String message = messageModel.message;

            ChatSentItemBinding chatSentItemBinding = (ChatSentItemBinding) binding;
            chatSentItemBinding.setMessage(message);
            chatSentItemBinding.setTime(time);

            if (messageModel.seen) {
                setMessageStatus(chatSentItemBinding, MESSAGE_SEEN);
            } else {
                setMessageStatus(chatSentItemBinding, MESSAGE_SENT);
            }

            chatSentItemBinding.executePendingBindings();

            if (!chatSentItemBinding.messageTextView.isInLayout()) {
                chatSentItemBinding.messageTextView.requestLayout();
            }
        }

        private void setMessageStatus(ChatSentItemBinding binding, String status) {
            switch (status) {
                case MESSAGE_SENT:
                    binding.sentImageView.setVisibility(View.VISIBLE);
                    binding.seenImageView.setVisibility(View.INVISIBLE);
                    break;

                case MESSAGE_SEEN:
                    binding.sentImageView.setVisibility(View.INVISIBLE);
                    binding.seenImageView.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }
}
