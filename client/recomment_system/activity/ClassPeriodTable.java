package client.recomment_system.activity;

import java.time.LocalTime;
import java.util.List;

/**
 * 학교 교시별 시작 시각 고정 표. 회의록 기준 예시 값(1교시 09:30, 이후 1시간 간격)으로
 * 채워뒀다 — 실제 시간표에 맞게 값만 조정하면 된다 (구조는 바꾸지 말 것).
 */
public final class ClassPeriodTable {
    public static final List<ClassPeriod> PERIODS = List.of(
            new ClassPeriod(1, LocalTime.of(9, 30), 60),
            new ClassPeriod(2, LocalTime.of(10, 30), 60),
            new ClassPeriod(3, LocalTime.of(11, 30), 60),
            new ClassPeriod(4, LocalTime.of(12, 30), 60),
            new ClassPeriod(5, LocalTime.of(13, 30), 60),
            new ClassPeriod(6, LocalTime.of(14, 30), 60),
            new ClassPeriod(7, LocalTime.of(15, 30), 60),
            new ClassPeriod(8, LocalTime.of(16, 30), 60),
            new ClassPeriod(9, LocalTime.of(17, 30), 60));

    private ClassPeriodTable() {
    }
}
