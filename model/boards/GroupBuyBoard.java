package model.boards;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 해당 해시태그가 달린 공동구매 글만 골라낸다. 태그는 대소문자까지 정확히 일치해야 한다.
     * parsePost가 항상 GroupBuyPost를 만들지만, 다른 경로로 Post가 섞여 들어와도
     * ClassCastException으로 죽지 않도록 instanceof로 거른다.
     */
    public List<Post> filterByHashtag(String hashtag) {
        return posts.stream()
                .filter(post -> post instanceof GroupBuyPost)
                .filter(post -> ((GroupBuyPost) post).getHashtags().contains(hashtag))
                .collect(Collectors.toList());
    }

    @Override
    protected Post parsePost(String line) {
        return GroupBuyPost.fromDataString(line);
    }
}
