package com.daewin.ibachat.timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class to convert a long-typed server timestamp to one we can "query" based on
 * the current time (i.e. today/yesterday/n-days etc.)
 */

public class TimestampInterpreter {

    private Date timestampDate;

    public TimestampInterpreter(Long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        timestampDate = calendar.getTime();
    }

    public String getFullDate() {
        SimpleDateFormat dateFormat
                = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss", Locale.ENGLISH);

        return dateFormat.format(timestampDate);
    }

    public String getTime() {
        SimpleDateFormat dateFormat
                = new SimpleDateFormat("hh:mma", Locale.ENGLISH);

        return dateFormat.format(timestampDate);
    }

}
