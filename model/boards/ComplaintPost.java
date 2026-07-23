package model.boards;

import java.time.LocalDateTime;

import model.DataFormat;
import model.User;

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

    /**
     * 민원은 관리자여도 내용을 고칠 수 없다 — 관리자는 <b>답변(댓글)만</b> 가능해야 한다.
     * canEdit는 이제 Post 기본 구현과 같지만(§Post.canEdit 참고), 다른 게시글과 달리
     * canDelete까지 작성자 본인만 허용해야 하므로 둘 다 명시적으로 오버라이드한다.
     * 서버(handlePostUpdate/removePost)와 GUI(수정·삭제 버튼 노출)가 모두 이 메서드를 보므로
     * 여기 한 곳이면 된다.
     */
    @Override
    public boolean canEdit(User requester) {
        return requester.getId().equals(authorId);
    }

    /** 삭제도 같은 이유로 작성자 본인만. 관리자가 남의 민원을 지우면 문의 기록이 사라진다. */
    @Override
    public boolean canDelete(User requester) {
        return requester.getId().equals(authorId);
    }

    @Override
    public String toDataString() {
        return String.join(DataFormat.FIELD_DELIM,
                baseDataString(), DataFormat.encode(category1), DataFormat.encode(category2),
                String.valueOf(answered));
    }

    public static ComplaintPost fromDataString(String line) {
        String[] f = splitFields(line);
        ComplaintPost post = new ComplaintPost(f[0], f[1], f[2], f[3], emptyToNull(f[4]), emptyToNull(f[5]),
                LocalDateTime.parse(f[6], DataFormat.DATETIME_FORMATTER),
                DataFormat.decode(f[8]), DataFormat.decode(f[9]));
        post.getComments().addAll(parseComments(f[7]));
        if (Boolean.parseBoolean(f[10])) {
            post.markAnswered();
        }
        return post;
    }
}
