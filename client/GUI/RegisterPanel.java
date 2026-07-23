package client.GUI;

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
    // 학과는 자유 텍스트가 아니라 단과대→학부→학과 3단 드롭다운으로 고른다 (오타로 학과
    // 게시판·공지 대상에서 빠지는 것을 막는다).
    private final DepartmentPickerPanel departmentPicker = new DepartmentPickerPanel(false);
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

        // 학과 픽커는 라벨 3개(단과대/학부/학과)를 자체적으로 그리므로 한 줄 전체를 차지한다.
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        form.add(departmentPicker, c);

        c.gridy = 4;
        c.insets = new Insets(0, 6, 0, 6);
        form.add(dormitoryCheckBox, c);

        JPanel buttons = new JPanel();
        buttons.add(registerButton);
        buttons.add(backButton);
        c.gridy = 5;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(14, 6, 0, 6);
        form.add(buttons, c);

        add(form, new GridBagConstraints());
    }

    private void attemptRegister() {
        String id = idField.getText();
        String department = departmentPicker.getSelectedDepartmentName();
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
        passwordField.setText("");
        dormitoryCheckBox.setSelected(false);
        // 학과 드롭다운은 비밀번호처럼 민감한 값이 아니라 그대로 둬도 무방하다.
    }
}
