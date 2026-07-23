package client.GUI;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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
    private final JButton backButton = new JButton("로그인으로");

    public RegisterPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        registerButton.addActionListener(e -> attemptRegister());
        backButton.addActionListener(e -> mainFrame.switchTo("login"));
        // 입력칸에서 엔터를 쳐도 가입되게 한다 (LoginPanel과 동일한 조작감).
        idField.addActionListener(e -> attemptRegister());
        departmentField.addActionListener(e -> attemptRegister());
        passwordField.addActionListener(e -> attemptRegister());
        initLayout();
    }

    /** 위 필드들을 이 패널(this)에 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        // LoginPanel과 같은 구성: GridBagLayout 위에 폼 하나만 올려 항상 가운데 정렬.
        setLayout(new GridBagLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints c = new GridBagConstraints();

        JLabel title = new JLabel("회원가입");
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
        form.add(new JLabel("학과"), c);
        c.gridy = 3;
        form.add(new JLabel("비밀번호"), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        idField.setColumns(16);
        departmentField.setColumns(16);
        passwordField.setColumns(16);
        c.gridy = 1;
        form.add(idField, c);
        c.gridy = 2;
        form.add(departmentField, c);
        c.gridy = 3;
        form.add(passwordField, c);

        c.gridy = 4;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 6, 0, 6);
        form.add(dormitoryCheckBox, c);

        // 학과명이 서버에 등록된 이름과 한 글자라도 다르면 학과 게시판에 들어갈 수 없다.
        JLabel hint = new JLabel("※ 학과는 서버에 등록된 학과명과 정확히 같아야 합니다");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(Color.GRAY);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 6, 0, 6);
        form.add(hint, c);

        JPanel buttons = new JPanel();
        buttons.add(registerButton);
        buttons.add(backButton);
        c.gridy = 6;
        c.insets = new Insets(14, 6, 0, 6);
        form.add(buttons, c);

        add(form, new GridBagConstraints());
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
            JOptionPane.showMessageDialog(this, "가입이 완료되었습니다. 로그인해 주세요.",
                    "가입 완료", JOptionPane.INFORMATION_MESSAGE);
            clearFields(); // 다음에 이 화면으로 다시 들어왔을 때 남의 입력이 남아있지 않도록
            mainFrame.switchTo("login");
        } else {
            passwordField.setText("");
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "가입 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        idField.setText("");
        departmentField.setText("");
        passwordField.setText("");
        dormitoryCheckBox.setSelected(false);
    }
}
