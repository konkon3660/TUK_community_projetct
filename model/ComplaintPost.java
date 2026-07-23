package model;

import java.time.LocalDateTime;

public class ComplaintPost extends Post {
    private static final long serialVersionUID = 1L;

    // 필드 순서(0~7은 Post 공통 필드): 8=category1, 9=category2, 10=answered
    private final String category1;
    private final String category2;
    private boolean answered;

    public ComplaintPost(String id, String title, String authorId, String content,
                          String filePath, String imagePath, LocalDateTime createdAt,
                          String category1, String category2) {
        super(id, title, authorId, content, filePath, imagePath, createdAt);
        this.category1 = category1;
        this.category2 = category2;
        this.answered = false;
    }

    public String getCategory1() {
        return category1;
    }

    public String getCategory2() {
        return category2;
    }

    public boolean isAnswered() {
        return answered;
    }

    /** 관리자가 답변 댓글을 addComment로 등록할 때 함께 호출해서 답변완료 상태로 전환 */
    public void markAnswered() {
        this.answered = true;
    }

    @Override
    public String toDataString() {
        return String.join(DataFormat.FIELD_DELIM,
                baseDataString(), category1, category2, String.valueOf(answered));
    }

    public static ComplaintPost fromDataString(String line) {
        String[] f = splitFields(line);
        ComplaintPost post = new ComplaintPost(f[0], f[1], f[2], f[3], emptyToNull(f[4]), emptyToNull(f[5]),
                LocalDateTime.parse(f[6], DataFormat.DATETIME_FORMATTER), f[8], f[9]);
        post.getComments().addAll(parseComments(f[7]));
        if (Boolean.parseBoolean(f[10])) {
            post.markAnswered();
        }
        return post;
    }
}
