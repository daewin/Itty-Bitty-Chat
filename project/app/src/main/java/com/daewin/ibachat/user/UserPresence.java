package com.daewin.ibachat.user;

import android.util.Log;

import com.daewin.ibachat.database.DatabaseUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import static com.daewin.ibachat.user.User.getCurrentUsersEncodedEmail;

/**
 * Singleton for user presence related functions
 * <p>
 * Bulk of the code structure is based off:
 * https://firebase.google.com/docs/database/android/offline-capabilities#section-sample
 */

public class UserPresence {

    private static UserPresence ourInstance;

    public static UserPresence getInstance() {
        if (ourInstance == null) {
            ourInstance = new UserPresence();
        }
        return ourInstance;
    }

    public static void clearInstance() {
        getInstance().removeUserPresence();
        ourInstance = null;
    }

    private String currentUsersEncodedEmail;

    private DatabaseReference userConnectionsReference;
    private DatabaseReference currentUsersConnectedReference;
    private DatabaseReference connectedReference;
    private DatabaseReference lastOnlineReference;

    private ValueEventListener currentUsersConnectedListener;

    private UserPresence() {
        String encodedEmail = getCurrentUsersEncodedEmail();

        if (encodedEmail != null) {
            currentUsersEncodedEmail = encodedEmail;
            initializeDatabaseReferences();
        }
    }

    private void initializeDatabaseReferences() {
        DatabaseReference database = DatabaseUtil.getDatabase().getReference();

        connectedReference = database.child(".info/connected");

        DatabaseReference usersDatabase
                = database.child("users").child(currentUsersEncodedEmail);

        userConnectionsReference = usersDatabase.child("connections");

        // Store the timestamp of the users last disconnect (i.e. last seen)
        lastOnlineReference = usersDatabase.child("lastSeen");
    }

    // Since the user can connect from multiple devices, we store each connection instance
    // separately. Any time the userConnectionsReference's value below is null (i.e. has
    // no children), the user is offline.
    public void establishUserPresence() {
        if (currentUsersEncodedEmail != null) {

            currentUsersConnectedListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Boolean connected = dataSnapshot.getValue(Boolean.class);

                    if (connected != null && connected) {
                        currentUsersConnectedReference = userConnectionsReference.push();

                        // When this device disconnects, remove it from the connection list
                        currentUsersConnectedReference.onDisconnect().removeValue();

                        // When this device disconnects, update the last seen value
                        lastOnlineReference.onDisconnect().setValue(ServerValue.TIMESTAMP);

                        // Add this device to my connections list
                        currentUsersConnectedReference.setValue(true);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w("Error", databaseError.toException().getMessage());
                }
            };

            connectedReference.addValueEventListener(currentUsersConnectedListener);
        }
    }

    public void removeUserPresence() {
        if (currentUsersConnectedListener != null) {
            connectedReference.removeEventListener(currentUsersConnectedListener);
        }
    }

    public void forceRemoveCurrentConnection() {
        if (currentUsersConnectedReference != null) {
            currentUsersConnectedReference.removeValue();
        }

        if (lastOnlineReference != null) {
            lastOnlineReference.setValue(ServerValue.TIMESTAMP);
        }


    }
}
