package com.daewin.ibachat;

import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.user.UserPresence;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lifecycle observer to ensure that our presence module works correctly when the app is in
 * the background (i.e. not visible and not in the foreground) and foreground, regardless of
 * how it got there (screen off/home button pressed/back button). Firebase performs all sync
 * functions when back online.
 */

public class MyLifecycleObserver implements DefaultLifecycleObserver {

    private static AtomicInteger numberOfActivities = new AtomicInteger();
    private static ValueEventListener mUserPresenceListener;

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {

        if(numberOfActivities.incrementAndGet() == 1){
            FirebaseDatabase.getInstance().goOnline();
            mUserPresenceListener = UserPresence.establishUserPresence();
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {

        if(numberOfActivities.decrementAndGet() == 0){
            FirebaseDatabase.getInstance().goOffline();

            if (mUserPresenceListener != null) {
                UserPresence.connectedReference.removeEventListener(mUserPresenceListener);
            }
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {

    }
}
