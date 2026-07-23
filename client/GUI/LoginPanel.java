package client.GUI;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import model.User;
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
    private final JButton registerButton = new JButton("회원가입");

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        loginButton.addActionListener(e -> attemptLogin());
        registerButton.addActionListener(e -> mainFrame.switchTo("register"));
        // 두 입력칸에서 엔터를 쳐도 로그인되게 한다 (버튼까지 마우스로 가지 않아도 되도록).
        idField.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        initLayout();
    }

    /** idField/passwordField/loginButton을 이 패널(this)에 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        // GridBagLayout에 폼 하나만 올려서 창 크기가 변해도 가운데에 머무르게 한다.
        setLayout(new GridBagLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);

        JLabel title = new JLabel("TUK Community");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(6, 6, 20, 6);
        form.add(title, c);

        c.gridwidth = 1;
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridy = 1;
        form.add(new JLabel("학번"), c);
        c.gridy = 2;
        form.add(new JLabel("비밀번호"), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        idField.setColumns(16);
        passwordField.setColumns(16);
        c.gridy = 1;
        form.add(idField, c);
        c.gridy = 2;
        form.add(passwordField, c);

        JPanel buttons = new JPanel();
        buttons.add(loginButton);
        buttons.add(registerButton);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(18, 6, 0, 6);
        form.add(buttons, c);

        add(form, new GridBagConstraints());
    }

    private void attemptLogin() {
        String id = idField.getText();
        String password = new String(passwordField.getPassword());
        Packet request = Packet.request(RequestType.LOGIN, new LoginRequest(id, password));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            User user = (User) response.getPayload();
            mainFrame.setCurrentUser(user);
            passwordField.setText(""); // 로그아웃 후 다시 돌아왔을 때 비번이 남아있지 않도록
            // 관리자는 학생 화면(home)에 들어가면 안 된다 — documents/gui.md §2
            mainFrame.switchTo(user.isAdmin() ? "admin" : "home");
        } else {
            passwordField.setText("");
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "로그인 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
