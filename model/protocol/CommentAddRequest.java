package model.protocol;

import java.io.Serializable;

import model.boards.Comment;

public class CommentAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String boardKey;
    private final String postId;
    private final Comment comment;

    public CommentAddRequest(String boardKey, String postId, Comment comment) {
        this.boardKey = boardKey;
        this.postId = postId;
        this.comment = comment;
    }

    public String getBoardKey() {
        return boardKey;
    }

    public String getPostId() {
        return postId;
    }

    public Comment getComment() {
        return comment;
    }
}
