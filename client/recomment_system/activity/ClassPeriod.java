package client.recomment_system.activity;

import java.time.LocalTime;

/** 교시 하나(시작 시각 + 길이). ClassPeriodTable의 고정 표에서만 생성된다. */
public class ClassPeriod {
    private final int periodIndex;
    private final LocalTime startTime;
    private final int durationMinutes;

    public ClassPeriod(int periodIndex, LocalTime startTime, int durationMinutes) {
        this.periodIndex = periodIndex;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }

    public int getPeriodIndex() {
        return periodIndex;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public LocalTime getEndTime() {
        return startTime.plusMinutes(durationMinutes);
    }
}
