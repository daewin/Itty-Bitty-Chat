package com.daewin.ibachat.user;

import com.daewin.ibachat.model.UserModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
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

    // Guarantees that the returned user model if available, exists.
    public static UserModel getCurrentUserModel() {
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser != null) {
            UserModel currentUser = new UserModel(currentFirebaseUser.getDisplayName(),
                    currentFirebaseUser.getEmail());

            if (currentUser.exists()) {
                return currentUser;
            }
        }

        return null;
    }

    public static String getCurrentUsersEncodedEmail() {

        UserModel currentUser = getCurrentUserModel();

        if (currentUser != null) {
            return currentUser.getEncodedEmail();
        }
        return null;
    }

    // If this user has just registered, create and fill their database
    // information into the Firebase Database.
    public static Task<Boolean> createUserDatabaseIfMissing() {

        final TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();

        UserModel currentUserModel = getCurrentUserModel();

        if (currentUserModel != null ) {
            final String name = currentUserModel.getName();
            final String encodedEmail = currentUserModel.getEncodedEmail();

            mDatabase.child("users_index").child(encodedEmail)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (!dataSnapshot.exists()) {
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
