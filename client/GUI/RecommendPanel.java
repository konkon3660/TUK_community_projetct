package client.GUI;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import client.recomment_system.activity.ActivityRecommender;
import client.recomment_system.book.BookRecommender;
import client.recomment_system.menu.MenuRecommender;

/** 추천 3종(할거/메뉴/책) 탭 화면. 서버 통신이 필요 없고 로컬 파일만 읽는다. */
public class RecommendPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTabbedPane tabs = new JTabbedPane();
    private final ActivityRecommender activityRecommender = new ActivityRecommender();
    private final MenuRecommender menuRecommender = new MenuRecommender();
    private final BookRecommender bookRecommender = new BookRecommender();
    private final JButton backButton = new JButton("뒤로");

    public RecommendPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        initLayout();
    }

    /** tabs에 할거/메뉴/책 3개 탭을 만들고 각 Recommender를 호출해 결과를 보여주는 부분 — 디자인은 자유.
     *  예: menuRecommender.recommendTwoRandom(), bookRecommender.recommendForDepartment(
     *  mainFrame.getCurrentUser().getDepartment()), activityRecommender.recommendNow(...)(현재 TODO). */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
