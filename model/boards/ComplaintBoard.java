package model.boards;

import java.util.List;
import java.util.stream.Collectors;

import model.User;

public class ComplaintBoard extends AbstractBoard {
    private static final String DATA_FILE_PATH = "server/data/boards/complaint_board.dat";

    /** 전체 민원함 열람은 관리자만 가능 */
    @Override
    public boolean canAccess(User user) {
        return user.isAdmin();
    }

    @Override
    public String getDataFilePath() {
        return DATA_FILE_PATH;
    }

    /** 일반 사용자의 "문의 내역" 탭에서 사용: 본인이 넣은 민원만 조회 */
    public List<Post> getPostsByAuthor(String userId) {
        return posts.stream()
                .filter(p -> p.getAuthorId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    protected Post parsePost(String line) {
        return ComplaintPost.fromDataString(line);
    }
}
