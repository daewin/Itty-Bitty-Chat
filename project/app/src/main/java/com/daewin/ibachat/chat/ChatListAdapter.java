package com.daewin.ibachat.chat;

import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daewin.ibachat.databinding.ChatReceivedItemBinding;
import com.daewin.ibachat.databinding.ChatSentItemBinding;
import com.daewin.ibachat.model.MessageModel;
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.timestamp.TimestampInterpreter;
import com.daewin.ibachat.user.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Recycler View Adapter for displaying a chat list
 */

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 0;
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;

    private ArrayList<MessageModel> messages;
    private String currentUsersEmail;
    private DatabaseReference threadMessagesReference;

    ChatListAdapter(ArrayList<MessageModel> messages, String threadID) {
        this.messages = messages;

        DatabaseReference databaseReference
                = FirebaseDatabase.getInstance().getReference();

        threadMessagesReference
                = databaseReference.child("thread_messages").child(threadID);

        UserModel userModel = User.getCurrentUserModel();
        if (userModel != null && userModel.exists()) {
            this.currentUsersEmail = userModel.getEmail();
        }
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
        MessageModel messageModel = messages.get(position);

        if (messageModel.getEmail().equals(currentUsersEmail)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        MessageModel messageModel = messages.get(position);
        holder.bindTo(messageModel);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position, List<Object> payloads) {
        MessageModel messageModel = messages.get(position);
        holder.bindTo(messageModel);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }


    class MessageViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;
        private int viewType;

        MessageViewHolder(ViewDataBinding binding, int viewType) {
            super(binding.getRoot());

            this.binding = binding;
            this.viewType = viewType;
        }

        void bindTo(MessageModel messageModel) {
            TimestampInterpreter interpreter
                    = new TimestampInterpreter(messageModel.getTimestamp());

            String interpretation = interpreter.getTimestampInterpretation();
            String time;

            switch (interpretation) {
                case TimestampInterpreter.TODAY:
                    time = "today at " + interpreter.getTime();
                    break;

                case TimestampInterpreter.YESTERDAY:
                    time = "yesterday at interpreter.getTime()";
                    break;

                default:
                    time = interpreter.getFullDate();
                    break;
            }

            setIndividualItemBinding(messageModel, time);
        }

        void setIndividualItemBinding(MessageModel messageModel, String time) {

            String message = messageModel.getMessage();

            if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {

                ChatReceivedItemBinding chatReceivedItemBinding = (ChatReceivedItemBinding) binding;

                chatReceivedItemBinding.setMessage(message);
                chatReceivedItemBinding.setTime(time);
                chatReceivedItemBinding.executePendingBindings();

                if(!chatReceivedItemBinding.messageTextView.isInLayout()){
                    chatReceivedItemBinding.messageTextView.requestLayout();
                }

                if(!messageModel.isSeen()){
                    String messageID = messageModel.getMessageID();
                    if(messageID != null && !messageID.isEmpty()) {
                        threadMessagesReference.child(messageID).child("seen").setValue(true);
                    }
                }

            } else {

                ChatSentItemBinding chatSentItemBinding = (ChatSentItemBinding) binding;

                chatSentItemBinding.setMessage(message);
                chatSentItemBinding.setTime(time);

                if(messageModel.isSeen()){
                    chatSentItemBinding.seenImageView.setVisibility(View.VISIBLE);
                } else {
                    chatSentItemBinding.seenImageView.setVisibility(View.INVISIBLE);
                }

                chatSentItemBinding.executePendingBindings();

                if(!chatSentItemBinding.messageTextView.isInLayout()){
                    chatSentItemBinding.messageTextView.requestLayout();
                }
            }
        }
    }
}
