package model;

import java.util.List;

public interface Board {
    List<Post> getPosts();

    void addPost(Post post);

    /** requester가 해당 게시글에 대한 삭제 권한(canDelete)이 없으면 예외 발생 */
    void removePost(String postId, User requester);

    boolean canAccess(User user);

    /** 이 게시판이 저장되는 .dat 파일 경로 (server/data/boards/ 기준 상대경로) */
    String getDataFilePath();
}
