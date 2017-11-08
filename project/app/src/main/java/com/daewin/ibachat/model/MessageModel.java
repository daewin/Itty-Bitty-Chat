package com.daewin.ibachat.model;

import android.support.annotation.Nullable;

import com.google.firebase.database.Exclude;

import java.util.Comparator;

/**
 * Message Model
 */

public class MessageModel {

    private @Exclude @Nullable String messageID;
    private String email;
    private String message;
    private Long timestamp;
    private boolean seen;

    public MessageModel() {
        // Default constructor required for calls to DataSnapshot.getValue(ThreadModel.class)
    }

    public MessageModel(String email, String message, Long timestamp) {
        this.email = email;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = false;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof MessageModel) {
            final MessageModel other = (MessageModel) obj;

            return other.getEmail().equals(this.getEmail())
                    && other.getMessage().equals(this.getMessage())
                    && other.getTimestamp().equals(this.getTimestamp());
        }

        return false;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    // Comparator for the Sorted List in the Adapter
    @Exclude
    public static final Comparator<MessageModel> timeComparator = new Comparator<MessageModel>() {
        @Override
        public int compare(MessageModel a, MessageModel b) {

            Long timeA = a.getTimestamp();
            Long timeB = b.getTimestamp();

            return timeB.compareTo(timeA);
        }
    };

    @Nullable
    @Exclude
    public String getMessageID() {
        return messageID;
    }

    @Exclude
    public void setMessageID(@Nullable String messageID) {
        this.messageID = messageID;
    }
}
