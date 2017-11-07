package com.daewin.ibachat.model;

import com.google.firebase.database.Exclude;

import java.util.Comparator;

/**
 * Message Model
 */

public class MessageModel {

    private String email;
    private String message;
    private Long timestamp;
    private Boolean seen;

    public MessageModel(){
        // Default constructor required for calls to DataSnapshot.getValue(ThreadModel.class)
    }

    public MessageModel(String email, String message, Long timestamp, Boolean seen) {
        this.email = email;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = seen;
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

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
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

}
