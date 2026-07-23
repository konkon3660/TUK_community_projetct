package model.boards;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import model.DataFormat;
import model.User;

/**
 * 모든 게시글 타입(자유/공동구매/공지/민원 등)의 공통 기반 클래스.
 * 추가 필드가 없는 게시글(자유/학과별/기숙사 게시판)은 이 클래스를 그대로 인스턴스화해서 쓴다.
 * 하위 클래스는 baseDataString()/splitFields()/parseComments()를 이용해
 * 공통 필드 + 자신만의 추가 필드를 이어 붙이는 방식으로 toDataString/fromDataString을 구현한다.
 */
public class Post implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final String id;
    protected String title;
    protected final String authorId;
    protected String content;
    protected String filePath; // null이면 첨부파일 없음
    protected String imagePath; // null이면 첨부이미지 없음
    protected final List<Comment> comments;
    protected final LocalDateTime createdAt;

    public Post(String id, String title, String authorId, String content,
                String filePath, String imagePath, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.authorId = authorId;
        this.content = content;
        this.filePath = filePath;
        this.imagePath = imagePath;
        this.createdAt = createdAt;
        this.comments = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    /** 삭제 권한(canDelete)을 확인한 뒤 댓글을 제거 */
    public void removeComment(Comment comment, User requester) {
        if (!comments.contains(comment)) {
            throw new NoSuchElementException("댓글을 찾을 수 없음");
        }
        if (!comment.canDelete(requester)) {
            throw new IllegalStateException("삭제 권한 없음: " + requester.getId());
        }
        comments.remove(comment);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 관리자이거나 본인 글일 때만 수정 가능 */
    public boolean canEdit(User requester) {
        return requester.isAdmin() || requester.getId().equals(authorId);
    }

    /** 관리자이거나 본인 글일 때만 삭제 가능 */
    public boolean canDelete(User requester) {
        return requester.isAdmin() || requester.getId().equals(authorId);
    }

    /** 추가 필드가 없는 게시글(자유/학과별/기숙사)의 기본 구현. 하위 클래스는 오버라이드해서 자기 필드를 이어붙인다. */
    public String toDataString() {
        return baseDataString();
    }

    /** toDataString()이 baseDataString() 그대로인 경우(추가 필드 없는 Post)를 복원 */
    public static Post fromDataString(String line) {
        String[] f = splitFields(line);
        Post post = new Post(f[0], f[1], f[2], f[3], emptyToNull(f[4]), emptyToNull(f[5]),
                LocalDateTime.parse(f[6], DataFormat.DATETIME_FORMATTER));
        post.getComments().addAll(parseComments(f[7]));
        return post;
    }

    /** 공통 필드 7개 + 인코딩된 댓글 목록을 FIELD_DELIM으로 이어붙임 (하위 클래스가 이어서 자기 필드를 붙임) */
    protected String baseDataString() {
        String commentsEncoded = comments.stream()
                .map(Comment::toDataString)
                .collect(Collectors.joining(DataFormat.SUBLIST_DELIM));
        return String.join(DataFormat.FIELD_DELIM,
                id, title, authorId, content,
                emptyIfNull(filePath), emptyIfNull(imagePath),
                createdAt.format(DataFormat.DATETIME_FORMATTER),
                commentsEncoded);
    }

    protected static String[] splitFields(String line) {
        return line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
    }

    protected static List<Comment> parseComments(String encoded) {
        List<Comment> result = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return result;
        }
        for (String part : encoded.split(Pattern.quote(DataFormat.SUBLIST_DELIM))) {
            result.add(Comment.fromDataString(part));
        }
        return result;
    }

    protected static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
