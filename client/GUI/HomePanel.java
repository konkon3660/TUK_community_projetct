package client.GUI;

import javax.swing.JButton;
import javax.swing.JPanel;

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
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openFreeBoard() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openGroupBuyBoard() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openDepartmentBoard() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openDormBoard() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openNoticeBoard() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openComplaint() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openChatRoomList() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void openRecommend() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void logout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
