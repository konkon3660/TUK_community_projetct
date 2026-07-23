package client.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import client.recomment_system.activity.Activity;
import client.recomment_system.activity.ActivityRecommender;
import client.recomment_system.activity.TimetableEntry;
import client.recomment_system.book.BookRecommendation;
import client.recomment_system.book.BookRecommender;
import client.recomment_system.menu.MenuOption;
import client.recomment_system.menu.MenuRecommender;
import model.User;

/** 추천 3종(할거/메뉴/책) 탭 화면. 서버 통신이 필요 없고 로컬 파일만 읽는다. */
public class RecommendPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTabbedPane tabs = new JTabbedPane();
    private final ActivityRecommender activityRecommender = new ActivityRecommender();
    private final MenuRecommender menuRecommender = new MenuRecommender();
    private final BookRecommender bookRecommender = new BookRecommender();
    private final JButton backButton = new JButton("뒤로");

    // 할거 탭
    private final JLabel activityStatusLabel = new JLabel();
    private final JLabel activityResultLabel = new JLabel();
    private final JButton activityRefreshButton = new JButton("할 거 다시 추천받기");
    private final JButton timetableEditButton = new JButton("시간표 입력/수정");

    // 메뉴 탭
    private final JTextArea todayMenuArea = new JTextArea();
    private final JLabel menuResultLabel = new JLabel();
    private final JLabel menuTipLabel = new JLabel();
    private final JButton menuRefreshButton = new JButton("메뉴 추천 다시 받기");

    // 책 탭
    private final JLabel bookResultLabel = new JLabel();
    private final JButton bookRefreshButton = new JButton("책 다시 추천받기");

    public RecommendPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        activityRefreshButton.addActionListener(e -> refreshActivity());
        timetableEditButton.addActionListener(e -> openTimetableEditor());
        menuRefreshButton.addActionListener(e -> refreshMenu());
        bookRefreshButton.addActionListener(e -> refreshBook());
        initLayout();
        // 생성자는 로그인 전에 돌기 때문에 여기서 getCurrentUser()를 보면 null이다(HomePanel과 동일).
        // 그래서 데이터 로드는 화면이 실제로 보여질 때 한다 — CardLayout.show()가 componentShown을 띄운다.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshAll();
            }
        });
    }

    /** tabs에 할거/메뉴/책 3개 탭을 만들고 각 Recommender를 호출해 결과를 보여주는 부분 — 디자인은 자유. */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        JLabel title = new JLabel("추천");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        tabs.addTab("할 거", buildActivityTab());
        tabs.addTab("메뉴", buildMenuTab());
        tabs.addTab("책", buildBookTab());
        add(tabs, BorderLayout.CENTER);

        JPanel foot = new JPanel();
        foot.add(backButton);
        add(foot, BorderLayout.SOUTH);
    }

    private JPanel buildActivityTab() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        activityStatusLabel.setForeground(Color.GRAY);
        activityResultLabel.setFont(activityResultLabel.getFont().deriveFont(Font.BOLD, 18f));

        body.add(activityStatusLabel);
        body.add(Box.createVerticalStrut(12));
        body.add(activityResultLabel);

        return wrapWithButton(body, activityRefreshButton, timetableEditButton);
    }

    private JPanel buildMenuTab() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        todayMenuArea.setEditable(false);
        todayMenuArea.setOpaque(false);
        body.add(new JScrollPane(todayMenuArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        menuResultLabel.setFont(menuResultLabel.getFont().deriveFont(Font.BOLD, 15f));
        menuTipLabel.setForeground(Color.GRAY);
        bottom.add(menuResultLabel);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(menuTipLabel);
        body.add(bottom, BorderLayout.SOUTH);

        return wrapWithButton(body, menuRefreshButton);
    }

    private JPanel buildBookTab() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        body.add(bookResultLabel, BorderLayout.NORTH);
        return wrapWithButton(body, bookRefreshButton);
    }

    /** 탭 3개가 전부 "내용 + 아래쪽 버튼 줄" 구조라 껍데기만 공통으로 뽑았다. */
    private JPanel wrapWithButton(JPanel body, JButton... buttons) {
        JPanel tab = new JPanel(new BorderLayout());
        JPanel buttonRow = new JPanel();
        for (JButton button : buttons) {
            buttonRow.add(button);
        }
        tab.add(body, BorderLayout.CENTER);
        tab.add(buttonRow, BorderLayout.SOUTH);
        return tab;
    }

    /** 시간표 편집 화면으로 이동. 돌아오면 componentShown이 다시 refreshAll()을 부르므로 새로고침은 필요 없다. */
    private void openTimetableEditor() {
        ((TimetableEditorPanel) mainFrame.getScreen("timetableEditor")).open();
        mainFrame.switchTo("timetableEditor");
    }

    private void refreshAll() {
        refreshActivity();
        refreshMenu();
        refreshBook();
    }

    /** 지금 공강이 몇 분 남았는지 안내하고, 그 안에 끝낼 수 있는 할 거 하나를 보여준다. */
    private void refreshActivity() {
        List<TimetableEntry> timetable;
        try {
            timetable = activityRecommender.loadTimetable();
        } catch (RuntimeException e) {
            activityStatusLabel.setText("시간표를 읽지 못했습니다.");
            activityResultLabel.setText(e.getMessage());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int freeMinutes = activityRecommender.freeMinutesAt(timetable, now);
        Activity activity = activityRecommender.recommendNow(timetable, now);

        if (freeMinutes == ActivityRecommender.IN_CLASS) {
            activityStatusLabel.setText("지금은 수업 시간입니다.");
            activityResultLabel.setText("수업 끝나고 다시 눌러주세요.");
            return;
        }
        if (freeMinutes == ActivityRecommender.UNLIMITED) {
            activityStatusLabel.setText("오늘 남은 수업이 없습니다 — 시간 제한 없이 골랐어요.");
        } else {
            activityStatusLabel.setText("다음 수업까지 " + freeMinutes + "분 남았습니다.");
        }
        if (activity == null) {
            // 목록이 비었거나, 남은 시간 안에 끝낼 수 있는 게 하나도 없는 경우
            activityResultLabel.setText("지금 시간에 할 만한 게 목록에 없어요.");
            return;
        }
        activityResultLabel.setText(activity.getContent() + "  (약 " + activity.getDurationMinutes() + "분)");
    }

    /** 오늘의 학식 + 랜덤 식당 2곳 + 꿀팁 한 줄. */
    private void refreshMenu() {
        try {
            List<String> todayMenu = menuRecommender.todayMenuText();
            todayMenuArea.setText(todayMenu.isEmpty()
                    ? "오늘의 학식 정보가 아직 등록되지 않았습니다."
                    : String.join(System.lineSeparator(), todayMenu));
            todayMenuArea.setCaretPosition(0);

            List<MenuOption> options = menuRecommender.recommendTwoRandom();
            if (options.isEmpty()) {
                menuResultLabel.setText("추천할 식당 목록이 비어 있습니다.");
            } else {
                StringBuilder sb = new StringBuilder("<html>학식 말고 이건 어때요?<br>");
                for (MenuOption option : options) {
                    sb.append("· ").append(option.getRestaurant())
                            .append(" — ").append(option.getMenuName()).append("<br>");
                }
                menuResultLabel.setText(sb.append("</html>").toString());
            }

            String tip = menuRecommender.randomTip();
            menuTipLabel.setText(tip.isEmpty() ? "" : "TIP. " + tip);
        } catch (RuntimeException e) {
            todayMenuArea.setText("메뉴 데이터를 읽지 못했습니다: " + e.getMessage());
            menuResultLabel.setText("");
            menuTipLabel.setText("");
        }
    }

    /** 로그인한 유저의 학과에 맞는 책 1권. 그 학과 책이 없으면 recommendForDepartment가 null을 준다. */
    private void refreshBook() {
        User user = mainFrame.getCurrentUser();
        if (user == null) {
            bookResultLabel.setText("로그인 후 이용할 수 있습니다.");
            return;
        }
        try {
            BookRecommendation book = bookRecommender.recommendForDepartment(user.getDepartment());
            if (book == null) {
                bookResultLabel.setText("<html>" + user.getDepartment() + " 추천 도서가 아직 등록되지 않았습니다.</html>");
                return;
            }
            bookResultLabel.setText("<html><b>" + book.getTitle() + "</b> (" + book.getDepartment() + ")<br><br>"
                    + book.getDescription() + "</html>");
        } catch (RuntimeException e) {
            bookResultLabel.setText("추천 도서를 읽지 못했습니다: " + e.getMessage());
        }
    }
}
