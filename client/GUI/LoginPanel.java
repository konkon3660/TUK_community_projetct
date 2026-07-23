package client.GUI;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import model.protocol.LoginRequest;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/**
 * 로그인 화면. 새 화면을 만들 때 참고할 기준 예시 — 화면 레이아웃(initLayout)은 팀원 자유이지만,
 * "버튼을 눌렀을 때 서버와 어떻게 대화하고 화면을 어떻게 전환하는지"는 이미 고정되어 있다.
 */
public class LoginPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField idField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("로그인");

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        loginButton.addActionListener(e -> attemptLogin());
        initLayout();
    }

    /** idField/passwordField/loginButton을 이 패널(this)에 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void attemptLogin() {
        String id = idField.getText();
        String password = new String(passwordField.getPassword());
        Packet request = Packet.request(RequestType.LOGIN, new LoginRequest(id, password));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // TODO: 구현 필요. 예: mainFrame.setCurrentUser((User) response.getPayload());
            //       mainFrame.switchTo("home");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "로그인 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
