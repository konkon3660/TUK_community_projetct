package client.recomment_system.book;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import model.FileStorage;

/** 학과별 추천 도서 중 무작위 1권 추천. 조회 + 필터 + 랜덤 선택뿐이라 완전 구현되어 있다. */
public class BookRecommender {
    private static final Path BOOKS_PATH = Path.of("client/recommend_data/book_recommend.dat");

    private final Random random = new Random();

    public List<BookRecommendation> loadAll() {
        List<BookRecommendation> result = new ArrayList<>();
        try {
            for (String line : FileStorage.readLines(BOOKS_PATH)) {
                if (!line.isEmpty()) {
                    result.add(BookRecommendation.fromDataString(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("추천 도서 로드 실패: " + BOOKS_PATH, e);
        }
        return result;
    }

    /** 해당 학과 책 목록 중 무작위 1권. 학과에 등록된 책이 없으면 빈 Optional이 아니라 null을 반환. */
    public BookRecommendation recommendForDepartment(String department) {
        List<BookRecommendation> matches = loadAll().stream()
                .filter(b -> b.getDepartment().equals(department))
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(random.nextInt(matches.size()));
    }
}
