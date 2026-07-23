package client.GUI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import model.boards.ComplaintPost;
import model.boards.GroupBuyPost;
import model.boards.Post;
import model.protocol.BoardKey;
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
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JButton newPostButton = new JButton("글쓰기");
    private final JButton openButton = new JButton("열기");
    private final JButton backButton = new JButton("뒤로");
    private final JLabel headerLabel = new JLabel();
    private final DefaultListModel<Post> listModel = new DefaultListModel<>();
    private final JList<Post> postJList = new JList<>(listModel);
    private String boardKey;
    private String backTarget;
    private List<Post> posts;

    public PostListPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        newPostButton.addActionListener(e -> openEditor());
        openButton.addActionListener(e -> openSelectedPost());
        backButton.addActionListener(e -> mainFrame.switchTo(backTarget));
        initLayout();
    }

    /** newPostButton/backButton과 게시글 목록을 배치하는 부분 — 디자인은 자유. 단,
     *  관리자가 게시글 관리/민원함 목적으로 연 경우(backTarget이 "admin")는 글쓰기가
     *  학생 행위이므로 newPostButton을 숨기거나 비활성화할 것. */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 20f));
        add(headerLabel, BorderLayout.NORTH);

        postJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        postJList.setCellRenderer(new PostRenderer());
        postJList.setFixedCellHeight(28);
        postJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 더블클릭으로도 열 수 있게
                    openSelectedPost();
                }
            }
        });
        add(new JScrollPane(postJList), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.add(openButton);
        buttons.add(newPostButton);
        buttons.add(backButton);
        add(buttons, BorderLayout.SOUTH);
    }

    /** 목록 한 줄의 표시 형식: 제목 · 작성자 · 작성시각 · 댓글 수 (+ 게시글 타입별 표시) */
    private static class PostRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            Post post = (Post) value;
            StringBuilder line = new StringBuilder();
            if (post instanceof ComplaintPost) {
                line.append(((ComplaintPost) post).isAnswered() ? "[답변완료] " : "[답변대기] ");
            } else if (post instanceof GroupBuyPost) {
                line.append("[최대 ").append(((GroupBuyPost) post).getMaxMembers()).append("명] ");
            }
            line.append(post.getTitle())
                    .append("   ·   ").append(post.getAuthorId())
                    .append("   ·   ").append(post.getCreatedAt().format(DISPLAY_TIME));
            if (!post.getComments().isEmpty()) {
                line.append("   ·   댓글 ").append(post.getComments().size());
            }
            return super.getListCellRendererComponent(list, line.toString(), index, isSelected, cellHasFocus);
        }
    }

    /** mainFrame.switchTo("postList") 전에 반드시 먼저 호출해서 어느 게시판을 보여줄지 지정한다.
     *  backTarget은 뒤로가기를 눌렀을 때 돌아갈 화면 이름 — 학생이 열 때는 "home", 관리자가
     *  게시글 관리/민원함으로 열 때는 "admin"을 넘긴다 (관리자가 학생 화면에 갇히지 않도록). */
    public void open(String boardKey, String backTarget) {
        this.boardKey = boardKey;
        this.backTarget = backTarget;
        refresh();
    }

    /**
     * 마지막으로 연 게시판을 서버에서 다시 받아온다. 이 화면은 posts를 들고 있는 사본이라
     * 상세/에디터에서 글을 저장·삭제하고 돌아오면 목록이 낡은 채로 보인다. 그래서 돌아오기
     * 직전에 이걸 부른다 (PostDetailPanel/PostEditorPanel/GroupBuyPostEditorPanel은 backTarget을
     * 모르기 때문에 open(boardKey, backTarget)을 대신 부를 수 없다).
     */
    public void refresh() {
        if (boardKey == null) {
            return; // 아직 한 번도 열지 않았으면 새로고침할 게시판이 없다
        }
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
        headerLabel.setText(boardTitle(boardKey) + " (" + posts.size() + ")");
        listModel.clear();
        for (Post post : posts) {
            listModel.addElement(post);
        }
        postJList.clearSelection();

        // 글쓰기가 맞지 않는 상황에서는 버튼을 숨긴다:
        // - 관리자가 관리 목적으로 연 목록(backTarget이 "admin") — 글쓰기는 학생 행위
        // - 공지: 작성은 관리자 화면에서, 민원: 접수는 ComplaintPanel에서 한다
        boolean adminView = "admin".equals(backTarget);
        boolean writtenElsewhere = BoardKey.NOTICE.equals(boardKey) || BoardKey.COMPLAINT.equals(boardKey);
        newPostButton.setVisible(!adminView && !writtenElsewhere);
    }

    /** 선택한 글을 상세 화면으로 넘긴다 (열기 버튼 / 목록 더블클릭 공용). */
    private void openSelectedPost() {
        Post selected = postJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "글을 먼저 선택하세요.", "열기", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ((PostDetailPanel) mainFrame.getScreen("postDetail")).open(boardKey, selected);
        mainFrame.switchTo("postDetail");
    }

    /** boardKey에 맞는 편집 화면(postEditor/groupBuyPostEditor/noticePostEditor)을 열고 전환. */
    private void openEditor() {
        if (BoardKey.GROUP_BUY.equals(boardKey)) {
            ((GroupBuyPostEditorPanel) mainFrame.getScreen("groupBuyPostEditor")).open(null);
            mainFrame.switchTo("groupBuyPostEditor");
        } else if (BoardKey.NOTICE.equals(boardKey)) {
            ((NoticePostEditorPanel) mainFrame.getScreen("noticePostEditor")).open(null);
            mainFrame.switchTo("noticePostEditor");
        } else if (BoardKey.COMPLAINT.equals(boardKey)) {
            mainFrame.switchTo("complaint"); // 민원은 전용 접수 화면이 따로 있다
        } else {
            // 자유/기숙사/학과 게시판은 추가 필드가 없는 Post라 공용 에디터를 쓴다.
            ((PostEditorPanel) mainFrame.getScreen("postEditor")).open(boardKey, null);
            mainFrame.switchTo("postEditor");
        }
    }

    /** 화면에 보여줄 게시판 이름. 학과 게시판은 boardKey가 곧 학과명이라 그대로 쓴다. */
    private String boardTitle(String boardKey) {
        switch (boardKey) {
            case BoardKey.FREE:
                return "자유게시판";
            case BoardKey.GROUP_BUY:
                return "공동구매";
            case BoardKey.DORM:
                return "기숙사게시판";
            case BoardKey.NOTICE:
                return "공지사항";
            case BoardKey.COMPLAINT:
                return mainFrame.getCurrentUser().isAdmin() ? "민원함" : "내 문의 내역";
            default:
                return boardKey;
        }
    }
}
