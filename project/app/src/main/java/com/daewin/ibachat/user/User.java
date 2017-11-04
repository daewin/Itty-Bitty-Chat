package com.daewin.ibachat.user;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Helper function for User related functions
 */

public class User {

    private static DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    // If this user has just registered, create and fill their database
    // information into the Firebase Database.
    public static Task<Boolean> createUserDatabaseIfMissing(final FirebaseUser currentUser) {

        final TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();

        String currentUsersEmail = currentUser.getEmail();

        if (currentUsersEmail != null) {
            final String name = currentUser.getDisplayName();
            final String encodedEmail = getEncodedEmail(currentUsersEmail);

            mDatabase.child("users").child(encodedEmail)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists()) {
                                initializeUserDatabaseInformation(name, encodedEmail);
                            }

                            taskCompletionSource.setResult(true);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            taskCompletionSource.setException(databaseError.toException());
                        }
                    });
        }

        return taskCompletionSource.getTask();
    }

    private static void initializeUserDatabaseInformation(String name, String encodedEmail) {

        mDatabase.child("users").child(encodedEmail).child("name").setValue(name);
        mDatabase.child("users_index").child(encodedEmail).child("name").setValue(name);
    }

    // Firebase Database doesn't allow period characters in the key, so we need to replace
    // it with a comma.
    public static String getEncodedEmail(String email) {

        return email.replace('.', ',');
    }

    public static String getDecodedEmail(String email) {

        return email.replace(',', '.');
    }

}
