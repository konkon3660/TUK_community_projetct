package client.GUI;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import model.protocol.BoardKey;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

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
        setLayout(new GridBagLayout());
        JPanel menu = new JPanel(new GridBagLayout());
        menu.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("관리자 메뉴");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(6, 6, 20, 6);
        menu.add(title, c);
        // 로그인한 관리자 이름은 여기 표시하지 않는다 — initLayout은 로그인 전(생성자)에 실행되어
        // mainFrame.getCurrentUser()가 아직 null이다.

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 6, 4, 6);
        int row = 1;
        for (JButton button : new JButton[] {
                writeNoticeButton, postManagementButton, complaintInboxButton, userEditButton }) {
            button.setPreferredSize(new Dimension(220, 40));
            c.gridy = row++;
            menu.add(button, c);
        }

        logoutButton.setPreferredSize(new Dimension(220, 32));
        c.gridy = row;
        c.insets = new Insets(20, 6, 4, 6);
        menu.add(logoutButton, c);

        add(menu, new GridBagConstraints());
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
        // 게시판 이름은 화면 표시용, 실제로 서버에 보내는 값은 BoardKey 상수다.
        // 민원함은 전용 버튼이 따로 있으므로 여기 목록에서는 뺀다.
        String[] labels = { "자유게시판", "공동구매", "기숙사게시판", "공지사항", "학과게시판(직접 입력)" };
        String[] keys = { BoardKey.FREE, BoardKey.GROUP_BUY, BoardKey.DORM, BoardKey.NOTICE, null };

        Object choice = JOptionPane.showInputDialog(this, "관리할 게시판을 고르세요", "게시글 관리",
                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
        if (choice == null) {
            return; // 취소
        }
        String boardKey = null;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(choice)) {
                boardKey = keys[i];
            }
        }
        if (boardKey == null) {
            // 학과 게시판은 학과가 늘어날 수 있어서 목록으로 고정하지 않고 직접 입력받는다.
            String department = JOptionPane.showInputDialog(this, "학과명을 입력하세요 (서버에 등록된 이름과 정확히 일치)");
            if (department == null || department.trim().isEmpty()) {
                return;
            }
            boardKey = department.trim();
        }
        // 뒤로가기가 학생 화면("home")이 아니라 관리자 홈으로 오도록 backTarget은 항상 "admin".
        ((PostListPanel) mainFrame.getScreen("postList")).open(boardKey, "admin");
        mainFrame.switchTo("postList");
    }

    private void openComplaintInbox() {
        ((PostListPanel) mainFrame.getScreen("postList")).open(BoardKey.COMPLAINT, "admin");
        mainFrame.switchTo("postList");
    }

    private void logout() {
        Packet response = mainFrame.getConnection().sendRequest(Packet.request(RequestType.LOGOUT, null));
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "로그아웃 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 서버 세션이 끊겼으므로 클라이언트가 들고 있던 유저도 함께 비운다.
        mainFrame.setCurrentUser(null);
        mainFrame.switchTo("login");
    }
}
