package model;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Board의 공통 동작(목록 관리, CRUD 권한 위임)을 구현. 하위 클래스는 canAccess()와
 * getDataFilePath()만 채우면 되도록 하여 게시판 타입이 늘어나도 반복 코드가 생기지 않게 한다.
 */
public abstract class AbstractBoard implements Board {
    protected final List<Post> posts = new ArrayList<>();

    @Override
    public List<Post> getPosts() {
        return posts;
    }

    @Override
    public void addPost(Post post) {
        posts.add(post);
    }

    @Override
    public void removePost(String postId, User requester) {
        Post target = findPost(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없음: " + postId));
        if (!target.canDelete(requester)) {
            throw new IllegalStateException("삭제 권한 없음: " + requester.getId());
        }
        posts.remove(target);
    }

    protected Optional<Post> findPost(String postId) {
        return posts.stream().filter(p -> p.getId().equals(postId)).findFirst();
    }
}
