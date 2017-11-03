package com.daewin.ibachat.user;

import android.support.annotation.NonNull;

import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;

import java.util.Comparator;

/**
 * SortedListAdapter obtained from: https://wrdlbrnft.github.io/SortedListAdapter/
 */

public class UserModel implements SortedListAdapter.ViewModel {

    public String name;
    public String username;
    public String email;

    public UserModel(){
        // Default constructor required for calls to DataSnapshot.getValue(UserModel.class)
    }

    public UserModel(String name, String username, String email) {
        this.name = name;
        this.username = username;
        this.email = email;
    }

    // This is used for purposes where email is unnecessary
    public UserModel(String name, String username) {
        this.name = name;
        this.username = username;
        this.email = "";
    }

    public boolean exists(){
        if(name != null && username != null && email != null){

            if(!name.isEmpty() && !username.isEmpty()){
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> boolean isSameModelAs(@NonNull T item) {

        if(item instanceof UserModel){
            final UserModel other = (UserModel) item;

            if(other.name.equals(name)
                    && other.username.equals(username)
                    && other.email.equals(email)){
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
    public static final Comparator<UserModel> alphabeticalComparator = new Comparator<UserModel>() {
        @Override
        public int compare(UserModel a, UserModel b) {
            return a.username.compareTo(b.username);
        }
    };
}
