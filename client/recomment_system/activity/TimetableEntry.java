package client.recomment_system.activity;

import java.util.regex.Pattern;

import model.DataFormat;

/** 시간표 한 칸: 무슨 요일(1=월 ~ 5=금) 몇 교시에 어떤 수업이 있는지. time_table.dat 한 줄 = 한 칸. */
public class TimetableEntry {
    private final int dayOfWeek;
    private final int periodIndex;
    private final String className;

    public TimetableEntry(int dayOfWeek, int periodIndex, String className) {
        this.dayOfWeek = dayOfWeek;
        this.periodIndex = periodIndex;
        this.className = className;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public int getPeriodIndex() {
        return periodIndex;
    }

    public String getClassName() {
        return className;
    }

    public String toDataString() {
        return String.join(DataFormat.FIELD_DELIM,
                String.valueOf(dayOfWeek), String.valueOf(periodIndex), className);
    }

    public static TimetableEntry fromDataString(String line) {
        String[] f = line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
        return new TimetableEntry(Integer.parseInt(f[0]), Integer.parseInt(f[1]), f[2]);
    }
}
