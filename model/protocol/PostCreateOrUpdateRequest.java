package model.protocol;

import java.io.Serializable;

import model.boards.Post;

/** boardKey로 대상 게시판을 식별 (예: "free", "dorm", "notice", "complaint", "groupbuy", 학과명 등) */
public class PostCreateOrUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String boardKey;
    private final Post post;

    public PostCreateOrUpdateRequest(String boardKey, Post post) {
        this.boardKey = boardKey;
        this.post = post;
    }

    public String getBoardKey() {
        return boardKey;
    }

    public Post getPost() {
        return post;
    }
}
