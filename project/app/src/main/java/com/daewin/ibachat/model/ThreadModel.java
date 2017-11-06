package com.daewin.ibachat.model;


/**
 * Model of a thread to store/retrieve thread information
 */

public class ThreadModel {

    private String lastMessage;
    private Long timestamp;

    public ThreadModel(){
        // Default constructor required for calls to DataSnapshot.getValue(ThreadModel.class)
    }

    public ThreadModel(String lastMessage, Long timestamp){
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }



}
