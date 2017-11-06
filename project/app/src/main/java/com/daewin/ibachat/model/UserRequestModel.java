package com.daewin.ibachat.model;

import android.support.annotation.NonNull;

import java.util.Comparator;

/**
 * SortedListAdapter obtained from: https://wrdlbrnft.github.io/SortedListAdapter/
 */

public class UserRequestModel extends UserModel {

    private Long timestamp;

    public UserRequestModel(String name, String email, Long timestamp) {
        super(name, email);
        this.timestamp = timestamp;
    }

    @Override
    public <T> boolean isSameModelAs(@NonNull T item) {

        if(item instanceof UserRequestModel){
            final UserRequestModel other = (UserRequestModel) item;

            if(other.getName().equals(getName())
                    && other.getEmail().equals(getEmail())
                    && (other.getTimestamp().compareTo(getTimestamp()) == 0)){
                return true;
            }
        }

        return false;
    }

    @Override
    public <T> boolean isContentTheSameAs(@NonNull T item) {
        return isSameModelAs(item);
    }

    // Comparator for the Sorted List in the Adapter
    public static final Comparator<UserRequestModel> timeComparator = new Comparator<UserRequestModel>() {
        @Override
        public int compare(UserRequestModel a, UserRequestModel b) {

            Long timeA = a.timestamp;
            Long timeB = b.timestamp;

            return timeB.compareTo(timeA);
        }
    };

    public Long getTimestamp() {
        return timestamp;
    }
}
