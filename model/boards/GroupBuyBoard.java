package model.boards;

import java.util.List;

import model.User;

public class GroupBuyBoard extends AbstractBoard {
    private static final String DATA_FILE_PATH = "server/data/boards/group_buying_board.dat";

    @Override
    public boolean canAccess(User user) {
        return true;
    }

    @Override
    public String getDataFilePath() {
        return DATA_FILE_PATH;
    }

    public List<Post> filterByHashtag(String hashtag) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    @Override
    protected Post parsePost(String line) {
        return GroupBuyPost.fromDataString(line);
    }
}
