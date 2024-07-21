package com.example.bmb.utils;

public class TimeAgoUtils {

    public static String getTimeAgo(long pastTimeMillis) {
        long currentTimeMillis = System.currentTimeMillis();
        long timeDifference = currentTimeMillis - pastTimeMillis;

        long minutes = timeDifference / (60 * 1000);
        long hours = minutes / 60;
        long days = hours / 24;

        String timeAgo;
        if (days > 0) {
            timeAgo = "hace " + days + (days == 1 ? " día" : " días");
        } else if (hours > 0) {
            timeAgo ="hace " +  hours + (hours == 1 ? " hora" : " horas");
        } else {
            timeAgo ="hace " +  minutes + (minutes == 1 ? " minuto" : " minutos");
        }
        return timeAgo;
    }
}
