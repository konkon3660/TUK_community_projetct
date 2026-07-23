package client.GUI;

import javax.swing.JButton;
import javax.swing.JPanel;

import model.protocol.BoardKey;

/**
 * 관리자의 진짜 첫 화면(로그인 성공 시 관리자는 "home"이 아니라 이 화면으로 바로 옴).
 * 관리 업무(공지 작성/게시글 관리/민원함/회원정보 수정)만 하고, 학생용 화면(자유게시판 글쓰기,
 * 채팅방 참여, 추천, 민원 제출 등)으로 가는 경로는 여기 없다 — 완전 분리.
 */
public class AdminPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JButton writeNoticeButton = new JButton("공지 작성");
    private final JButton postManagementButton = new JButton("게시글 관리");
    private final JButton complaintInboxButton = new JButton("민원함");
    private final JButton userEditButton = new JButton("회원정보 수정");
    private final JButton logoutButton = new JButton("로그아웃");

    public AdminPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        writeNoticeButton.addActionListener(e -> openWriteNotice());
        postManagementButton.addActionListener(e -> openPostManagement());
        complaintInboxButton.addActionListener(e -> openComplaintInbox());
        userEditButton.addActionListener(e -> mainFrame.switchTo("userEdit"));
        logoutButton.addActionListener(e -> logout());
        initLayout();
    }

    /** 위 버튼들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openWriteNotice() {
        ((NoticePostEditorPanel) mainFrame.getScreen("noticePostEditor")).open(null);
        mainFrame.switchTo("noticePostEditor");
    }

    /** 어느 게시판을 관리할지 고르게 하는 부분은 자유(레이아웃 문제) — 고른 뒤에는
     *  ((PostListPanel) mainFrame.getScreen("postList")).open(boardKey, "admin") 후
     *  switchTo("postList")로 연결한다. 뒤로가기가 관리자 홈("admin")으로 오도록 backTarget을
     *  꼭 "admin"으로 넘길 것 — 학생용 "home"으로 넘기면 관리자가 학생 화면에 갇힌다. */
    private void openPostManagement() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openComplaintInbox() {
        ((PostListPanel) mainFrame.getScreen("postList")).open(BoardKey.COMPLAINT, "admin");
        mainFrame.switchTo("postList");
    }

    private void logout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
