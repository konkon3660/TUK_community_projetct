package client.recomment_system.activity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import model.FileStorage;

/**
 * 시간표 + 할거 목록을 읽어 지금 이 순간의 공강 시간에 맞는 할 거리를 추천한다.
 *
 * <p><b>공강 판단 규칙</b> (08_status.md §3-3에서 "팀 논의 전"으로 남아 있던 부분 —
 * 여기서 아래와 같이 정했다. 바꿀 거면 {@link #freeMinutesAt}만 고치면 된다):
 * <ul>
 *   <li>공강 = <b>다음 수업 시작 전까지 남은 시간</b>. "연속된 빈 교시 전체"를 세지 않는 이유는,
 *       중간에 수업이 끼면 어차피 그 전까지밖에 못 놀기 때문이다.</li>
 *   <li>지금이 수업 중인 교시 안이면 공강이 아니다 → 추천하지 않는다({@code null}).</li>
 *   <li>오늘 남은 수업이 없으면(하교 후 · 오늘 수업 없음 · 주말) 시간 제한 없음
 *       ({@link #UNLIMITED}) — 모든 할 거가 후보가 된다.</li>
 *   <li>조건에 맞는 할 거가 하나도 없으면 예외 대신 {@code null}을 반환한다
 *       ({@code BookRecommender.recommendForDepartment}와 같은 컨벤션).</li>
 * </ul>
 */
public class ActivityRecommender {
    private static final Path ACTIVITIES_PATH = Path.of("client/recommend_data/active_recommend.dat");
    private static final Path TIMETABLE_PATH = Path.of("client/recommend_data/time_table.dat");

    /** {@link #freeMinutesAt}: 지금은 수업 중이라 공강이 아님. */
    public static final int IN_CLASS = 0;
    /** {@link #freeMinutesAt}: 오늘 남은 수업이 없어 시간 제한이 없음. */
    public static final int UNLIMITED = Integer.MAX_VALUE;

    private final Random random = new Random();

    public List<Activity> loadActivities() {
        List<Activity> result = new ArrayList<>();
        for (String line : readLines(ACTIVITIES_PATH)) {
            if (!line.isEmpty()) {
                result.add(Activity.fromDataString(line));
            }
        }
        return result;
    }

    public List<TimetableEntry> loadTimetable() {
        List<TimetableEntry> result = new ArrayList<>();
        for (String line : readLines(TIMETABLE_PATH)) {
            if (!line.isEmpty()) {
                result.add(TimetableEntry.fromDataString(line));
            }
        }
        return result;
    }

    /** 시간표 편집 화면(TimetableEditorPanel)이 저장 버튼에서 호출. 파일 전체를 덮어쓴다. */
    public void saveTimetable(List<TimetableEntry> timetable) {
        List<String> lines = new ArrayList<>();
        for (TimetableEntry entry : timetable) {
            lines.add(entry.toDataString());
        }
        try {
            FileStorage.writeLines(TIMETABLE_PATH, lines);
        } catch (IOException e) {
            throw new UncheckedIOException("시간표 저장 실패: " + TIMETABLE_PATH, e);
        }
    }

    /**
     * now 시각 기준으로 timetable 상의 공강 시간을 계산해서, 그 시간 안에 끝낼 수 있는 할 거리를 추천한다.
     * 후보가 여럿이면 그 중 무작위 1개 — 같은 공강에서 버튼을 다시 눌러도 다른 게 나오게 하기 위함이다.
     *
     * @return 추천할 할 거. 수업 중이거나 남은 시간 안에 끝낼 수 있는 게 없으면 {@code null}.
     */
    public Activity recommendNow(List<TimetableEntry> timetable, LocalDateTime now) {
        int freeMinutes = freeMinutesAt(timetable, now);
        if (freeMinutes == IN_CLASS) {
            return null;
        }
        List<Activity> candidates = new ArrayList<>();
        for (Activity activity : loadActivities()) {
            if (activity.getDurationMinutes() <= freeMinutes) {
                candidates.add(activity);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * now 시점의 공강이 몇 분 남았는지. 화면에서 "다음 수업까지 N분" 같은 안내를 띄우거나,
     * 추천이 {@code null}일 때 그 이유(수업 중인지 / 할 게 없는지)를 구분하는 데 쓴다.
     *
     * @return 남은 공강 분. 수업 중이면 {@link #IN_CLASS}, 오늘 남은 수업이 없으면 {@link #UNLIMITED}.
     */
    public int freeMinutesAt(List<TimetableEntry> timetable, LocalDateTime now) {
        // DayOfWeek.getValue()는 월=1 ~ 일=7이라 TimetableEntry의 요일(1=월~5=금)과 그대로 맞는다.
        // 주말이면 오늘 수업이 하나도 안 잡히므로 자연히 UNLIMITED가 된다.
        int today = now.getDayOfWeek().getValue();
        LocalTime time = now.toLocalTime();

        Set<Integer> busyPeriods = new HashSet<>();
        for (TimetableEntry entry : timetable) {
            if (entry.getDayOfWeek() == today) {
                busyPeriods.add(entry.getPeriodIndex());
            }
        }

        LocalTime nextClassStart = null;
        for (ClassPeriod period : ClassPeriodTable.PERIODS) {
            if (!busyPeriods.contains(period.getPeriodIndex())) {
                continue; // 수업이 없는 교시는 공강이므로 건너뛴다
            }
            if (!time.isBefore(period.getStartTime()) && time.isBefore(period.getEndTime())) {
                return IN_CLASS;
            }
            if (time.isBefore(period.getStartTime())
                    && (nextClassStart == null || period.getStartTime().isBefore(nextClassStart))) {
                nextClassStart = period.getStartTime();
            }
        }
        if (nextClassStart == null) {
            return UNLIMITED;
        }
        return (int) Duration.between(time, nextClassStart).toMinutes();
    }

    private List<String> readLines(Path path) {
        try {
            return FileStorage.readLines(path);
        } catch (IOException e) {
            throw new UncheckedIOException("추천 데이터 로드 실패: " + path, e);
        }
    }
}
