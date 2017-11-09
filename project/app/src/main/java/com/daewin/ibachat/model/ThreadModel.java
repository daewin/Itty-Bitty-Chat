package com.daewin.ibachat.model;


import android.support.annotation.NonNull;

import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.firebase.database.Exclude;

import java.util.Comparator;

/**
 * Model of a thread to store/retrieve thread information
 */

public class ThreadModel implements SortedListAdapter.ViewModel {

    private @Exclude String email;
    private String id;
    private String name;
    private String lastMessage;
    private Long timestamp;

    public ThreadModel(){
        // Default constructor required for calls to DataSnapshot.getValue(ThreadModel.class)
    }

    @Override
    public <T> boolean isSameModelAs(@NonNull T item) {
        if(item instanceof ThreadModel){
            final ThreadModel other = (ThreadModel) item;

            if(other.id.equals(id) && other.name.equals(name)){
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> boolean isContentTheSameAs(@NonNull T t) {
        return isSameModelAs(t);
    }

    // Comparator for the Sorted List in the Adapter
    @Exclude
    public static final Comparator<ThreadModel> timeComparator = new Comparator<ThreadModel>() {
        @Override
        public int compare(ThreadModel a, ThreadModel b) {

            Long timeA = a.timestamp;
            Long timeB = b.timestamp;

            return timeB.compareTo(timeA);
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Exclude
    public String getEmail() {
        return email;
    }

    @Exclude
    public void setEmail(String email) {
        this.email = email;
    }
}
