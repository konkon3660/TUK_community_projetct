package client.recomment_system.activity;

import java.util.regex.Pattern;

import model.DataFormat;

/** 할거 추천 목록의 항목 하나. active_recommend.dat 한 줄 = 할 거 하나. */
public class Activity {
    private final String content;
    private final int durationMinutes;

    public Activity(String content, int durationMinutes) {
        this.content = content;
        this.durationMinutes = durationMinutes;
    }

    public String getContent() {
        return content;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String toDataString() {
        return String.join(DataFormat.FIELD_DELIM, content, String.valueOf(durationMinutes));
    }

    public static Activity fromDataString(String line) {
        String[] f = line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
        return new Activity(f[0], Integer.parseInt(f[1]));
    }
}
