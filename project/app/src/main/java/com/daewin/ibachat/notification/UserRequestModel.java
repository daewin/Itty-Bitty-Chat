package com.daewin.ibachat.notification;

import android.support.annotation.NonNull;
import com.daewin.ibachat.user.UserModel;

import java.util.Comparator;

/**
 * SortedListAdapter obtained from: https://wrdlbrnft.github.io/SortedListAdapter/
 */

public class UserRequestModel extends UserModel {

    private long timestamp;

    UserRequestModel(String name, String email, long timestamp) {
        super(name, email);
        this.timestamp = timestamp;
    }

    @Override
    public <T> boolean isSameModelAs(@NonNull T item) {

        if(item instanceof UserRequestModel){
            final UserRequestModel other = (UserRequestModel) item;

            if(other.getName().equals(getName())
                    && other.getEmail().equals(getEmail())
                    && (Long.compare(other.timestamp, timestamp) == 0)){
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
    static final Comparator<UserRequestModel> timeComparator = new Comparator<UserRequestModel>() {
        @Override
        public int compare(UserRequestModel a, UserRequestModel b) {

            Long timeA = a.timestamp;
            Long timeB = b.timestamp;

            return timeB.compareTo(timeA);
        }
    };
}
