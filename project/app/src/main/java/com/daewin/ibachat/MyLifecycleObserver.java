package com.daewin.ibachat;

import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.daewin.ibachat.database.DatabaseUtil;
import com.daewin.ibachat.user.UserPresence;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lifecycle observer to ensure that our presence module works correctly when the app is in
 * the background (i.e. not visible and not in the foreground) and foreground, regardless of
 * how it got there (screen off/home button pressed/back button). Firebase performs all sync
 * functions when back online.
 */

public class MyLifecycleObserver implements DefaultLifecycleObserver {

    private static AtomicInteger numberOfActivities = new AtomicInteger();

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {

        if (numberOfActivities.incrementAndGet() == 1) {
            DatabaseUtil.getDatabase().goOnline();

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                UserPresence.getInstance().establishUserPresence();
            }
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {

        if (numberOfActivities.decrementAndGet() == 0) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                UserPresence.getInstance().removeUserPresence();
            }

            DatabaseUtil.getDatabase().goOffline();
        }
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {

    }
}
