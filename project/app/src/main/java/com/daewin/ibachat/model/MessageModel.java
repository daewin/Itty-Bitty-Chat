package com.daewin.ibachat.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Comparator;

/**
 * Message Model
 */

public class MessageModel implements SortedListAdapter.ViewModel {

    public @Exclude String messageID;
    public String email;
    public String message;
    public Object timestamp;
    public Boolean seen;

    public MessageModel() {
        // Default constructor required for calls to DataSnapshot.getValue(MessageModel.class)
    }

    public MessageModel(String email, String message) {
        this.email = email;
        this.message = message;
        this.timestamp = ServerValue.TIMESTAMP;
        this.seen = false;
    }

    @Exclude
    public static final Comparator<MessageModel> timeComparator = new Comparator<MessageModel>() {

        @Override
        public int compare(MessageModel a, MessageModel b) {

            Long aTimestamp = a.getLiveTimestamp();
            Long bTimestamp = b.getLiveTimestamp();

            if (aTimestamp != null && bTimestamp != null) {
                return bTimestamp.compareTo(aTimestamp);

            } else {
                Log.w("Error", "Comparing incorrect timestamp types");
                throw new ClassCastException();
            }
        }
    };

    @Exclude
    public boolean isTimestampLive() {
        try {
            Long longTimestamp = Long.class.cast(timestamp);

            if (longTimestamp != null) {
                return true;
            }

        } catch (ClassCastException e) {
            Log.w("Error", e.getMessage());
        }

        return false;
    }

    @Exclude
    @Nullable
    public Long getLiveTimestamp() {
        if (isTimestampLive()) {
            return Long.class.cast(timestamp);
        }

        return null;
    }

    @Exclude
    @Override
    public <T> boolean isSameModelAs(@NonNull T t) {
        if (t instanceof MessageModel) {
            final MessageModel other = (MessageModel) t;

            return other.messageID.equals(this.messageID);
        }
        return false;
    }

    @Exclude
    @Override
    public <T> boolean isContentTheSameAs(@NonNull T t) {
        if (t instanceof MessageModel) {
            final MessageModel other = (MessageModel) t;

            return other.email.equals(this.email)
                    && other.message.equals(this.message)
                    && other.timestamp.equals(this.timestamp)
                    && other.seen == this.seen;
        }
        return false;
    }
}
