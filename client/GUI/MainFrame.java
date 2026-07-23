package client.GUI;

import java.awt.CardLayout;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import client.CT.ServerConnection;

/**
 * 화면 전환 셸. CardLayout으로 화면(JPanel)들을 등록해두고 이름으로 전환한다.
 * ServerConnection을 하나만 만들어서 들고 있고, 모든 화면은 getConnection()으로 공유해서 쓴다.
 * 새 화면을 추가하는 방법은 documents/gui.md의 LoginPanel 예시를 참고할 것.
 */
public class MainFrame extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private final ServerConnection connection;
    private final JPanel screens = new JPanel(new CardLayout());

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

    /** 화면을 이름으로 등록. 여러 번 등록하면 마지막 것으로 덮어써진다. */
    public void registerScreen(String name, JPanel panel) {
        screens.add(panel, name);
    }

    /** 등록된 이름으로 화면을 전환. 전환되는 화면이 서버 푸시를 받아야 하면
     *  전환 직후 getConnection().setPushListener(...)를 그 화면에서 다시 등록할 것. */
    public void switchTo(String name) {
        ((CardLayout) screens.getLayout()).show(screens, name);
    }

    public static void main(String[] args) throws IOException {
        ServerConnection connection = new ServerConnection(SERVER_HOST, SERVER_PORT);
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(connection);
            frame.registerScreen("login", new LoginPanel(frame));
            frame.switchTo("login");
            frame.setVisible(true);
        });
    }
}
