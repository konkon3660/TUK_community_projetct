package client.GUI;

import javax.swing.JButton;
import javax.swing.JPanel;

/** 관리자 허브: 공지 작성/민원함/회원정보 수정으로 가는 화면. HomePanel에서 관리자일 때만 진입. */
public class AdminPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JButton writeNoticeButton = new JButton("공지 작성");
    private final JButton complaintInboxButton = new JButton("민원함");
    private final JButton userEditButton = new JButton("회원정보 수정");
    private final JButton backButton = new JButton("뒤로");

    public AdminPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        writeNoticeButton.addActionListener(e -> openWriteNotice());
        complaintInboxButton.addActionListener(e -> openComplaintInbox());
        userEditButton.addActionListener(e -> mainFrame.switchTo("userEdit"));
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        initLayout();
    }

    /** 위 버튼들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** ((NoticePostEditorPanel) mainFrame.getScreen("noticePostEditor")).open(null) 후 switchTo("noticePostEditor"). */
    private void openWriteNotice() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** ((PostListPanel) mainFrame.getScreen("postList")).open(BoardKey.COMPLAINT) 후 switchTo("postList"). */
    private void openComplaintInbox() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
