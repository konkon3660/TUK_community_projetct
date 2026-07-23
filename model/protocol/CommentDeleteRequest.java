package model.protocol;

import java.io.Serializable;

/** commentIndex는 해당 게시글의 comments 리스트 내 위치(0부터 시작) */
public class CommentDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String boardKey;
    private final String postId;
    private final int commentIndex;

    public CommentDeleteRequest(String boardKey, String postId, int commentIndex) {
        this.boardKey = boardKey;
        this.postId = postId;
        this.commentIndex = commentIndex;
    }

    public String getBoardKey() {
        return boardKey;
    }

    public String getPostId() {
        return postId;
    }

    public int getCommentIndex() {
        return commentIndex;
    }
}
