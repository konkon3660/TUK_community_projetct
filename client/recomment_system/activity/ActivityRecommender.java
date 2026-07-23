package client.recomment_system.activity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import model.FileStorage;

/** 시간표 + 할거 목록을 읽어 지금 이 순간의 공강 시간에 맞는 할 거리를 추천한다. */
public class ActivityRecommender {
    private static final Path ACTIVITIES_PATH = Path.of("client/recommend_data/active_recommend.dat");
    private static final Path TIMETABLE_PATH = Path.of("client/recommend_data/time_table.dat");

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

    /** now 시각 기준으로 timetable 상의 공강 시간을 계산해서, 그 시간 안에 끝낼 수 있는 할 거리를 추천한다. */
    public Activity recommendNow(List<TimetableEntry> timetable, LocalDateTime now) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private List<String> readLines(Path path) {
        try {
            return FileStorage.readLines(path);
        } catch (IOException e) {
            throw new UncheckedIOException("추천 데이터 로드 실패: " + path, e);
        }
    }
}
