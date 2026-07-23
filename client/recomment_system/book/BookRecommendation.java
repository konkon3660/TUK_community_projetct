package client.recomment_system.book;

import java.util.regex.Pattern;

import model.DataFormat;

/** 학과별 추천 도서 하나. book_recommend.dat 한 줄 = 책 하나. */
public class BookRecommendation {
    private final String department;
    private final String title;
    private final String description;

    public BookRecommendation(String department, String title, String description) {
        this.department = department;
        this.title = title;
        this.description = description;
    }

    public String getDepartment() {
        return department;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String toDataString() {
        return String.join(DataFormat.FIELD_DELIM, department, title, description);
    }

    public static BookRecommendation fromDataString(String line) {
        String[] f = line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
        return new BookRecommendation(f[0], f[1], f[2]);
    }
}
