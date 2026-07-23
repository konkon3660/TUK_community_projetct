package client.GUI;

import java.awt.CardLayout;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import client.CT.ServerConnection;
import model.User;

/**
 * 화면 전환 셸. CardLayout으로 화면(JPanel)들을 등록해두고 이름으로 전환한다.
 * ServerConnection을 하나만 만들어서 들고 있고, 모든 화면은 getConnection()으로 공유해서 쓴다.
 * 로그인한 유저도 여기 하나만 들고 있다가 setCurrentUser/getCurrentUser로 공유한다.
 * 새 화면을 추가하는 방법과 open(...) 컨벤션은 documents/06_gui.md의 LoginPanel 예시를 참고할 것.
 */
public class MainFrame extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private final ServerConnection connection;
    private final JPanel screens = new JPanel(new CardLayout());
    private final Map<String, JPanel> screensByName = new HashMap<>();
    private User currentUser;

    public MainFrame(ServerConnection connection) {
        super("TUK Community");
        this.connection = connection;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        add(screens);
    }

    public ServerConnection getConnection() {
        return connection;
    }

    /** LOGIN 응답으로 받은 User를 세션 동안 보관. LoginPanel.attemptLogin() 성공 분기에서 호출. */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /** 지금 로그인한 유저. 로그인 전에는 null. */
    public User getCurrentUser() {
        return currentUser;
    }

    /** 화면을 이름으로 등록. 여러 번 등록하면 마지막 것으로 덮어써진다. */
    public void registerScreen(String name, JPanel panel) {
        screens.add(panel, name);
        screensByName.put(name, panel);
    }

    /** 등록된 이름의 화면 인스턴스를 꺼낸다. switchTo 전에 그 화면의 open(...)을 호출해서
     *  보여줄 데이터를 넘길 때 사용한다
     *  (예: ((PostListPanel) mainFrame.getScreen("postList")).open(boardKey, "home")). */
    public JPanel getScreen(String name) {
        return screensByName.get(name);
    }

    /** 등록된 이름으로 화면을 전환. 전환되는 화면이 서버 푸시를 받아야 하면
     *  전환 직후 getConnection().setPushListener(...)를 그 화면에서 다시 등록할 것. */
    public void switchTo(String name) {
        ((CardLayout) screens.getLayout()).show(screens, name);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : SERVER_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : SERVER_PORT;
        ServerConnection connection;
        try {
            connection = new ServerConnection(host, port);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "서버 연결 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(connection);
            frame.registerScreen("login", new LoginPanel(frame));
            frame.registerScreen("register", new RegisterPanel(frame));
            frame.registerScreen("home", new HomePanel(frame));
            frame.registerScreen("boardMenu", new BoardMenuPanel(frame));
            frame.registerScreen("postList", new PostListPanel(frame));
            frame.registerScreen("postDetail", new PostDetailPanel(frame));
            frame.registerScreen("postEditor", new PostEditorPanel(frame));
            frame.registerScreen("groupBuyPostEditor", new GroupBuyPostEditorPanel(frame));
            frame.registerScreen("noticePostEditor", new NoticePostEditorPanel(frame));
            frame.registerScreen("complaint", new ComplaintPanel(frame));
            frame.registerScreen("chatRoomList", new ChatRoomListPanel(frame));
            frame.registerScreen("chatRoomCreate", new ChatRoomCreatePanel(frame));
            frame.registerScreen("chatRoom", new ChatRoomPanel(frame));
            frame.registerScreen("userEdit", new UserEditPanel(frame));
            frame.registerScreen("admin", new AdminPanel(frame));
            frame.registerScreen("recommend", new RecommendPanel(frame));
            frame.registerScreen("timetableEditor", new TimetableEditorPanel(frame));
            frame.switchTo("login");
            frame.setVisible(true);
        });
    }
}
