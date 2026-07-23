package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NoticePost extends Post {
    // 필드 순서(0~7은 Post 공통 필드): 8=targetDepartments, 9=dormNotice
    private final List<String> targetDepartments; // 비어있으면 전체 공지
    private final boolean dormNotice;

    public NoticePost(String id, String title, String authorId, String content,
                       String filePath, String imagePath, LocalDateTime createdAt,
                       List<String> targetDepartments, boolean dormNotice) {
        super(id, title, authorId, content, filePath, imagePath, createdAt);
        this.targetDepartments = targetDepartments;
        this.dormNotice = dormNotice;
    }

    public List<String> getTargetDepartments() {
        return targetDepartments;
    }

    public boolean isDormNotice() {
        return dormNotice;
    }

    /** 대상 학과/기숙사 지정에 따라 이 유저에게 보여야 하는 공지인지 판단 */
    public boolean isVisibleTo(User user) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    @Override
    public String toDataString() {
        String deptsEncoded = String.join(DataFormat.LIST_DELIM, targetDepartments);
        return String.join(DataFormat.FIELD_DELIM,
                baseDataString(), deptsEncoded, String.valueOf(dormNotice));
    }

    public static NoticePost fromDataString(String line) {
        String[] f = splitFields(line);
        List<String> depts = f[8].isEmpty()
                ? new ArrayList<>()
                : Arrays.stream(f[8].split(DataFormat.LIST_DELIM)).collect(Collectors.toList());
        NoticePost post = new NoticePost(f[0], f[1], f[2], f[3], emptyToNull(f[4]), emptyToNull(f[5]),
                LocalDateTime.parse(f[6], DataFormat.DATETIME_FORMATTER),
                depts, Boolean.parseBoolean(f[9]));
        post.getComments().addAll(parseComments(f[7]));
        return post;
    }
}
