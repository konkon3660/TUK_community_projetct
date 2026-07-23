package model.boards;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import model.DataFormat;
import model.User;

public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String authorId;
    private final String content;
    private final LocalDateTime createdAt;

    public Comment(String authorId, String content, LocalDateTime createdAt) {
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean canDelete(User requester) {
        return requester.isAdmin() || requester.getId().equals(authorId);
    }

    /** 댓글 하나를 SUBOBJECT_DELIM으로 인코딩 (게시글/채팅방의 댓글 목록 직렬화에 사용).
     *  댓글 내용은 '^'와 ';' 두 겹 안쪽에 있어서 인코딩이 특히 중요하다 — 여기서 encode를
     *  빼먹으면 댓글에 세미콜론 하나만 써도 게시글 전체를 못 읽게 된다. */
    public String toDataString() {
        return String.join(DataFormat.SUBOBJECT_DELIM,
                DataFormat.encode(authorId), DataFormat.encode(content),
                createdAt.format(DataFormat.DATETIME_FORMATTER));
    }

    public static Comment fromDataString(String data) {
        String[] f = data.split(Pattern.quote(DataFormat.SUBOBJECT_DELIM), -1);
        return new Comment(DataFormat.decode(f[0]), DataFormat.decode(f[1]),
                LocalDateTime.parse(f[2], DataFormat.DATETIME_FORMATTER));
    }
}
