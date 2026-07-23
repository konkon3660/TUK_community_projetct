package model.boards;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import model.FileStorage;
import model.User;

/**
 * Board의 공통 동작(목록 관리, CRUD 권한 위임, 파일 로드/저장)을 구현. 하위 클래스는
 * canAccess(), getDataFilePath(), parsePost()만 채우면 되도록 하여 게시판 타입이
 * 늘어나도 반복 코드가 생기지 않게 한다.
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

    @Override
    public void load() {
        posts.clear();
        try {
            for (String line : FileStorage.readLines(Path.of(getDataFilePath()))) {
                if (!line.isEmpty()) {
                    posts.add(parsePost(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("게시판 로드 실패: " + getDataFilePath(), e);
        }
    }

    @Override
    public void save() {
        try {
            FileStorage.writeLines(Path.of(getDataFilePath()),
                    posts.stream().map(Post::toDataString).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new UncheckedIOException("게시판 저장 실패: " + getDataFilePath(), e);
        }
    }

    /** 이 게시판이 다루는 게시글 타입에 맞는 fromDataString을 호출해서 한 줄을 Post로 되돌린다. */
    protected abstract Post parsePost(String line);
}
