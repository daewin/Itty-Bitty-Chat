package com.daewin.ibachat.timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Helper class to convert a long-typed server timestamp to one we can "query" based on
 * the current time (i.e. today/yesterday/n-days etc.)
 */

public class TimestampInterpreter {

    public static final String TODAY = "today";
    public static final String YESTERDAY = "yesterday";
    private static final String SOMEDAY = "someday";

    private Long timestamp;

    public TimestampInterpreter(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestampInterpretation() {

        Calendar timestampCalendar = getTimestampCalendar();
        Calendar currentCalendar = getCurrentCalendar();

        // Timestamp values
        int timestampYear = timestampCalendar.get(Calendar.YEAR);
        int timestampMonth = timestampCalendar.get(Calendar.MONTH);
        int timestampDate = timestampCalendar.get(Calendar.DATE);

        // Current time values
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentDate = currentCalendar.get(Calendar.DATE);

        if (timestampYear == currentYear) {
            if (timestampMonth == currentMonth) {
                if (timestampDate == currentDate) {
                    return TODAY;
                } else if (timestampDate == currentDate - 1) {
                    return YESTERDAY;
                }
            }
        }

        return SOMEDAY;
    }

    public String getFullDate() {
        SimpleDateFormat dateFormat
                = new SimpleDateFormat("dd-MM-yyyy h:mm a", Locale.getDefault());

        return dateFormat.format(getTimestampCalendar().getTime());
    }

    public String getTime() {
        SimpleDateFormat dateFormat
                = new SimpleDateFormat("h:mm a", Locale.getDefault());

        return dateFormat.format(getTimestampCalendar().getTime());
    }

    private Calendar getTimestampCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        return calendar;
    }

    private Calendar getCurrentCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        return calendar;
    }

}
