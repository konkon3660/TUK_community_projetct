package client.GUI;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;

import model.protocol.BoardKey;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/**
 * 로그인 성공 후 첫 화면(학생 전용 — 관리자는 이 화면에 오지 않고 "admin"으로 바로 간다).
 * 게시판 6종/채팅방/추천/로그아웃으로 가는 허브. 서버 통신은 없고 순수 네비게이션만 담당한다 —
 * 각 버튼이 어느 화면을 여는지는 TODO. 다른 화면으로 데이터와 함께 전환할 때는
 * mainFrame.getScreen("이름")을 캐스팅해서 open(...)을 먼저 호출한 뒤 mainFrame.switchTo("이름")을
 * 부르는 컨벤션을 따른다 (documents/gui.md 참고).
 */
public class HomePanel extends JPanel {
    private final MainFrame mainFrame;
    private final JButton freeBoardButton = new JButton("자유게시판");
    private final JButton groupBuyBoardButton = new JButton("공동구매");
    private final JButton departmentBoardButton = new JButton("학과게시판");
    private final JButton dormBoardButton = new JButton("기숙사게시판");
    private final JButton noticeBoardButton = new JButton("공지사항");
    private final JButton complaintButton = new JButton("민원");
    private final JButton chatRoomListButton = new JButton("채팅방");
    private final JButton recommendButton = new JButton("추천");
    private final JButton logoutButton = new JButton("로그아웃");

    public HomePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        freeBoardButton.addActionListener(e -> openFreeBoard());
        groupBuyBoardButton.addActionListener(e -> openGroupBuyBoard());
        departmentBoardButton.addActionListener(e -> openDepartmentBoard());
        dormBoardButton.addActionListener(e -> openDormBoard());
        noticeBoardButton.addActionListener(e -> openNoticeBoard());
        complaintButton.addActionListener(e -> openComplaint());
        chatRoomListButton.addActionListener(e -> openChatRoomList());
        recommendButton.addActionListener(e -> openRecommend());
        logoutButton.addActionListener(e -> logout());
        initLayout();
    }

    /** 위 버튼들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        setLayout(new GridBagLayout());
        JPanel menu = new JPanel(new GridBagLayout());
        menu.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints c = new GridBagConstraints();

        JLabel title = new JLabel("TUK Community");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(6, 6, 20, 6);
        menu.add(title, c);
        // 관리자 버튼은 없다 — 관리자는 로그인 직후 "admin"으로 가고 이 화면에 오지 않는다
        // (documents/gui.md §2 학생/관리자 완전 분리). 또 initLayout은 생성자에서(=로그인 전에)
        // 돌기 때문에 여기서 getCurrentUser()를 보면 null이다.

        // 게시판 6종을 2열로, 그 아래 채팅방/추천, 맨 아래 로그아웃.
        c.gridwidth = 1;
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        JButton[] buttons = {
                freeBoardButton, groupBuyBoardButton,
                departmentBoardButton, dormBoardButton,
                noticeBoardButton, complaintButton,
                chatRoomListButton, recommendButton };
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setPreferredSize(new Dimension(160, 44));
            c.gridx = i % 2;
            c.gridy = 1 + i / 2;
            menu.add(buttons[i], c);
        }

        logoutButton.setPreferredSize(new Dimension(160, 32));
        c.gridx = 0;
        c.gridy = 1 + buttons.length / 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(20, 4, 4, 4);
        menu.add(logoutButton, c);

        add(menu, new GridBagConstraints());
    }

    private void openFreeBoard() {
        openBoard(BoardKey.FREE);
    }

    private void openGroupBuyBoard() {
        openBoard(BoardKey.GROUP_BUY);
    }

    private void openDepartmentBoard() {
        // 학과 게시판은 별도 상수 없이 학과명 자체가 boardKey다 (BoardKey.java 주석 참고).
        openBoard(mainFrame.getCurrentUser().getDepartment());
    }

    private void openDormBoard() {
        // 비기숙사생이 눌러도 서버가 거부하고 그 사유가 목록 화면에 표시된다.
        openBoard(BoardKey.DORM);
    }

    private void openNoticeBoard() {
        openBoard(BoardKey.NOTICE);
    }

    /** 게시판 6종이 같은 목록 화면을 공유한다. 학생이 열었으므로 뒤로가기는 항상 "home". */
    private void openBoard(String boardKey) {
        ((PostListPanel) mainFrame.getScreen("postList")).open(boardKey, "home");
        mainFrame.switchTo("postList");
    }

    private void openComplaint() {
        // ComplaintPanel은 넘겨줄 데이터가 없어서 open(...) 없이 바로 전환한다.
        mainFrame.switchTo("complaint");
    }

    private void openChatRoomList() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openRecommend() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void logout() {
        Packet response = mainFrame.getConnection().sendRequest(Packet.request(RequestType.LOGOUT, null));
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "로그아웃 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 서버 세션이 끊겼으므로 클라이언트가 들고 있던 유저도 함께 비운다 (AdminPanel.logout과 동일).
        mainFrame.setCurrentUser(null);
        mainFrame.switchTo("login");
    }
}
