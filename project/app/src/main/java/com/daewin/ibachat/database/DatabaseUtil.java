package com.daewin.ibachat.database;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Entry point for database access to allow persistence to be set only once before
 * any calls to the Database is made.
 */

public class DatabaseUtil {

    private static FirebaseDatabase mDatabase;

    public static FirebaseDatabase getDatabase(){

        if(mDatabase == null){
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
        }

        return mDatabase;
    }
}
