package model.boards;

import java.util.List;
import java.util.stream.Collectors;

import model.User;

public class NoticeBoard extends AbstractBoard {
    private static final String DATA_FILE_PATH = "server/data/boards/notice_board.dat";

    @Override
    public boolean canAccess(User user) {
        return true;
    }

    @Override
    public String getDataFilePath() {
        return DATA_FILE_PATH;
    }

    /** 공지 작성은 관리자만 가능 (조회는 canAccess로 전체 허용) */
    public boolean canWrite(User user) {
        return user.isAdmin();
    }

    /** 이 유저의 학과/기숙사 여부에 맞는 공지만 필터링해서 반환 */
    public List<Post> getVisiblePosts(User user) {
        return posts.stream()
                .filter(p -> ((NoticePost) p).isVisibleTo(user))
                .collect(Collectors.toList());
    }

    @Override
    protected Post parsePost(String line) {
        return NoticePost.fromDataString(line);
    }
}
