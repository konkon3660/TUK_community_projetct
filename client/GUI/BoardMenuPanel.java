package client.GUI;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import model.protocol.BoardKey;

/**
 * 홈의 "게시판" 대분류를 눌렀을 때 나오는 세부 게시판 선택 화면.
 * 공지사항은 여기 없다 — 홈 대분류에 그대로 남겨두기로 했다 (민원도 마찬가지로 전용 화면).
 * 서버 통신 없이 순수 네비게이션만 담당한다 (HomePanel과 동일한 구성).
 */
public class BoardMenuPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JButton freeBoardButton = new JButton("자유게시판");
    private final JButton groupBuyBoardButton = new JButton("공동구매게시판");
    private final JButton departmentBoardButton = new JButton("학과게시판");
    private final JButton dormBoardButton = new JButton("기숙사게시판");
    private final JButton backButton = new JButton("뒤로");

    public BoardMenuPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        freeBoardButton.addActionListener(e -> openBoard(BoardKey.FREE));
        groupBuyBoardButton.addActionListener(e -> openBoard(BoardKey.GROUP_BUY));
        // 학과 게시판은 별도 상수 없이 학과명 자체가 boardKey다 (BoardKey.java 주석 참고).
        departmentBoardButton.addActionListener(
                e -> openBoard(mainFrame.getCurrentUser().getDepartment()));
        // 비기숙사생이 눌러도 서버가 거부하고 그 사유가 목록 화면에 표시된다.
        dormBoardButton.addActionListener(e -> openBoard(BoardKey.DORM));
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        initLayout();
    }

    /** 위 버튼들을 배치하는 부분 — 디자인은 자유 (HomePanel과 같은 세로 메뉴). */
    private void initLayout() {
        setLayout(new GridBagLayout());
        JPanel menu = new JPanel(new GridBagLayout());
        menu.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints c = new GridBagConstraints();

        JLabel title = new JLabel("게시판");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(6, 6, 20, 6);
        menu.add(title, c);

        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        JButton[] buttons = {
                freeBoardButton, groupBuyBoardButton, departmentBoardButton, dormBoardButton };
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setPreferredSize(new Dimension(220, 44));
            c.gridy = 1 + i;
            menu.add(buttons[i], c);
        }

        backButton.setPreferredSize(new Dimension(220, 32));
        c.gridy = 1 + buttons.length;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(20, 4, 4, 4);
        menu.add(backButton, c);

        add(menu, new GridBagConstraints());
    }

    /** 세부 게시판 4종이 같은 목록 화면을 공유한다. 이 화면에서 열었으므로 뒤로가기는 "boardMenu". */
    private void openBoard(String boardKey) {
        ((PostListPanel) mainFrame.getScreen("postList")).open(boardKey, "boardMenu");
        mainFrame.switchTo("postList");
    }
}
