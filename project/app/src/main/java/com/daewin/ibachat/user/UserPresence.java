package com.daewin.ibachat.user;

import com.daewin.ibachat.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

/**
 * Helper function for user presence related functions
 * <p>
 * Bulk of the code structure is based off:
 * https://firebase.google.com/docs/database/android/offline-capabilities#section-sample
 */

public class UserPresence {

    public static DatabaseReference database = FirebaseDatabase.getInstance().getReference();
    public static DatabaseReference connectedReference = database.child(".info/connected");


    public static ValueEventListener establishUserPresence() {

        // Since the user can connect from multiple devices, we store each connection instance
        // separately. Any time the userConnectionsReference's value below is null (i.e. has
        // no children), the user is offline.
        String encodedEmail = getCurrentUsersEncodedEmail();

        if(encodedEmail != null) {
            DatabaseReference usersDatabase
                    = database.child("users").child(encodedEmail);

            final DatabaseReference userConnectionsReference = usersDatabase.child("connections");

            // Store the timestamp of the users last disconnect (i.e. last seen)
            final DatabaseReference lastOnlineReference = usersDatabase.child("lastSeen");

            ValueEventListener connectedEventListener;
            connectedEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Boolean connected = dataSnapshot.getValue(Boolean.class);

                    if (connected != null && connected) {
                        DatabaseReference connectedReference = userConnectionsReference.push();

                        // When this device disconnects, remove it from the connection list
                        connectedReference.onDisconnect().removeValue();

                        // When this device disconnects, update the last seen value
                        lastOnlineReference.onDisconnect().setValue(ServerValue.TIMESTAMP);

                        // Add this device to my connections list
                        connectedReference.setValue(true);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // TODO
                }
            };

            connectedReference.addValueEventListener(connectedEventListener);

            return connectedEventListener;
        }

        return null;
    }

    public static void removeUserPresence(){
        String encodedEmail = getCurrentUsersEncodedEmail();

        if(encodedEmail != null) {

            DatabaseReference userConnectionsReference
                    = database.child("users").child(encodedEmail).child("connections");

            userConnectionsReference.removeValue();
        }
    }

    private static String getCurrentUsersEncodedEmail() {
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser != null) {
            UserModel currentUser = new UserModel(currentFirebaseUser.getDisplayName(),
                    currentFirebaseUser.getEmail());

            if (currentUser.exists()) {
                return User.getEncodedEmail(currentUser.getEmail());
            }
        }

        return null;
    }
}
