package com.daewin.ibachat.model;

import android.support.annotation.NonNull;

import com.daewin.ibachat.user.User;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.firebase.database.Exclude;

import java.util.Comparator;

/**
 * SortedListAdapter obtained from: https://wrdlbrnft.github.io/SortedListAdapter/
 */

public class UserModel implements SortedListAdapter.ViewModel {

    private String name;
    private String email;

    public UserModel(){
        // Default constructor required for calls to DataSnapshot.getValue(UserModel.class)
    }

    public UserModel(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Exclude
    public String getEncodedEmail() {
        return User.getEncodedEmail(getEmail());
    }

    @Exclude
    public boolean exists(){
        if(name != null && email != null){

            if(!name.isEmpty() && !email.isEmpty()){
                return true;
            }
        }
        return false;
    }

    @Exclude
    @Override
    public <T> boolean isSameModelAs(@NonNull T item) {

        if(item instanceof UserModel){
            final UserModel other = (UserModel) item;

            if(other.name.equals(name) && other.email.equals(email)){
                return true;
            }
        }

        return false;
    }

    @Exclude
    @Override
    public <T> boolean isContentTheSameAs(@NonNull T item) {
        return isSameModelAs(item);
    }

    // Comparator for the Sorted List in the Adapter
    @Exclude
    public static final Comparator<UserModel> alphabeticalComparator = new Comparator<UserModel>() {
        @Override
        public int compare(UserModel a, UserModel b) {
            return a.email.compareTo(b.email);
        }
    };
}
