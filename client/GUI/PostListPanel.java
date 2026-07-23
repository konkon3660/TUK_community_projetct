package client.GUI;

import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import model.boards.Post;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/**
 * 게시글 목록. 자유/공동구매/학과/기숙사/공지/민원 게시판이 전부 이 화면 하나를 공유한다
 * (boardKey만 다르게 넘기면 됨 — BoardKey.* 상수 또는 학과명 문자열).
 */
@SuppressWarnings("unchecked")
public class PostListPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JButton newPostButton = new JButton("글쓰기");
    private final JButton backButton = new JButton("뒤로");
    private String boardKey;
    private List<Post> posts;

    public PostListPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        newPostButton.addActionListener(e -> openEditor());
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        initLayout();
    }

    /** newPostButton/backButton과 게시글 목록을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** mainFrame.switchTo("postList") 전에 반드시 먼저 호출해서 어느 게시판을 보여줄지 지정한다. */
    public void open(String boardKey) {
        this.boardKey = boardKey;
        Packet request = Packet.request(RequestType.POST_LIST, boardKey);
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            this.posts = (List<Post>) response.getPayload();
            renderPosts();
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "게시글 목록 조회 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** posts를 화면에 그리고, 항목을 클릭하면
     *  ((PostDetailPanel) mainFrame.getScreen("postDetail")).open(boardKey, post) 후 switchTo("postDetail") — 렌더링은 자유. */
    private void renderPosts() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** boardKey에 맞는 편집 화면(postEditor/groupBuyPostEditor/noticePostEditor)을 열고 전환. */
    private void openEditor() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
