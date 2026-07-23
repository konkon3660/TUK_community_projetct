package model.protocol;

import java.io.Serializable;

public class PostDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String boardKey;
    private final String postId;

    public PostDeleteRequest(String boardKey, String postId) {
        this.boardKey = boardKey;
        this.postId = postId;
    }

    public String getBoardKey() {
        return boardKey;
    }

    public String getPostId() {
        return postId;
    }
}
