package client.GUI;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import model.User;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 회원가입 화면. LoginPanel과 같은 패턴 — 레이아웃(initLayout)은 자유, 서버 통신은 고정. */
public class RegisterPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField idField = new JTextField();
    private final JTextField departmentField = new JTextField();
    private final JCheckBox dormitoryCheckBox = new JCheckBox("기숙사생");
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton registerButton = new JButton("가입하기");

    public RegisterPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        registerButton.addActionListener(e -> attemptRegister());
        initLayout();
    }

    /** 위 필드들을 이 패널(this)에 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void attemptRegister() {
        String id = idField.getText();
        String department = departmentField.getText();
        boolean dormitory = dormitoryCheckBox.isSelected();
        String password = new String(passwordField.getPassword());
        // 회원가입으로 만드는 계정은 관리자가 아니다.
        User newUser = new User(id, department, dormitory, password, false);
        Packet request = Packet.request(RequestType.REGISTER, newUser);
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // TODO: 구현 필요. 예: mainFrame.switchTo("login");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "가입 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
