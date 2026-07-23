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

/** 관리자 전용: 학번으로 회원을 찾아 학과/기숙사 여부/비밀번호를 수정한다. */
public class UserEditPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField searchIdField = new JTextField();
    private final JButton searchButton = new JButton("조회");
    // 학과는 자유 텍스트가 아니라 단과대→학부→학과 3단 드롭다운으로 고른다 (RegisterPanel과 동일).
    private final DepartmentPickerPanel departmentPicker = new DepartmentPickerPanel(false);
    private final JCheckBox dormitoryCheckBox = new JCheckBox("기숙사생");
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton saveButton = new JButton("저장");
    private final JButton backButton = new JButton("관리자 홈");
    private final JLabel resultLabel = new JLabel(" ");
    private final JLabel legacyDepartmentWarning = new JLabel(" ");
    private User editingUser;

    public UserEditPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        searchButton.addActionListener(e -> search());
        saveButton.addActionListener(e -> save());
        backButton.addActionListener(e -> mainFrame.switchTo("admin"));
        searchIdField.addActionListener(e -> search()); // 학번 입력 후 엔터로 조회
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        setLayout(new GridBagLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints c = new GridBagConstraints();

        JLabel title = new JLabel("회원정보 수정");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(6, 6, 20, 6);
        form.add(title, c);

        // 1행: 학번 + 조회 버튼
        c.gridwidth = 1;
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridy = 1;
        form.add(new JLabel("학번"), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        searchIdField.setColumns(14);
        form.add(searchIdField, c);
        c.gridx = 2;
        c.fill = GridBagConstraints.NONE;
        form.add(searchButton, c);

        // 조회 결과 안내 (조회 전에는 아래 입력칸이 비활성)
        resultLabel.setFont(resultLabel.getFont().deriveFont(11f));
        resultLabel.setForeground(Color.GRAY);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 6, 10, 6);
        form.add(resultLabel, c);

        // 학과 픽커는 라벨 3개(단과대/학부/학과)를 자체적으로 그리므로 한 줄 전체를 차지한다.
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(6, 6, 0, 6);
        form.add(departmentPicker, c);

        // 트리에 없는 값(과거 자유 입력 데이터)을 조회했을 때만 보이는 경고.
        legacyDepartmentWarning.setFont(legacyDepartmentWarning.getFont().deriveFont(11f));
        legacyDepartmentWarning.setForeground(Color.RED);
        c.gridy = 4;
        c.insets = new Insets(0, 6, 6, 6);
        form.add(legacyDepartmentWarning, c);

        c.gridwidth = 1;
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 0;
        c.gridy = 6;
        form.add(new JLabel("새 비밀번호"), c);

        c.gridx = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        passwordField.setColumns(16);
        c.gridy = 5;
        c.fill = GridBagConstraints.NONE;
        form.add(dormitoryCheckBox, c);
        c.gridy = 6;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(passwordField, c);

        JLabel hint = new JLabel("※ 비밀번호를 비워두면 기존 비밀번호를 유지합니다");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(Color.GRAY);
        c.gridy = 6;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(2, 6, 0, 6);
        form.add(hint, c);

        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(backButton);
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(16, 6, 0, 6);
        form.add(buttons, c);

        add(form, new GridBagConstraints());
        setEditingEnabled(false); // 조회 전에는 수정할 대상이 없다
    }

    /** 학번으로 회원 1명을 조회한다 (USER_LOOKUP — 이 화면 때문에 추가한 관리자 전용 요청). */
    private void search() {
        String userId = searchIdField.getText().trim();
        if (userId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "학번을 입력하세요.", "조회", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Packet request = Packet.request(RequestType.USER_LOOKUP, userId);
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            editingUser = (User) response.getPayload();
            boolean found = departmentPicker.setSelection(editingUser.getDepartment());
            // 옛날 자유 입력 시절 값이라 트리에 없으면 드롭다운은 첫 항목 그대로고, 저장하면
            // 그 첫 항목으로 바뀐다 — 관리자가 알아채도록 경고를 띄운다.
            legacyDepartmentWarning.setText(found ? " "
                    : "⚠ 현재 값 \"" + editingUser.getDepartment() + "\"은 목록에 없습니다. 아래에서 새로 골라야 저장됩니다.");
            dormitoryCheckBox.setSelected(editingUser.isDormitory());
            passwordField.setText(""); // 기존 비밀번호는 화면에 띄우지 않는다
            resultLabel.setText("조회됨: " + editingUser.getId()
                    + (editingUser.isAdmin() ? " (관리자 계정)" : ""));
            setEditingEnabled(true);
        } else {
            editingUser = null;
            resultLabel.setText(" ");
            setEditingEnabled(false);
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "조회 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 조회에 성공하기 전까지는 수정/저장을 막는다 (save()가 editingUser를 그대로 쓰기 때문). */
    private void setEditingEnabled(boolean enabled) {
        departmentPicker.setEnabled(enabled);
        dormitoryCheckBox.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        if (!enabled) {
            dormitoryCheckBox.setSelected(false);
            passwordField.setText("");
            legacyDepartmentWarning.setText(" ");
        }
    }

    private void save() {
        editingUser.setDepartment(departmentPicker.getSelectedDepartmentName());
        editingUser.setDormitory(dormitoryCheckBox.isSelected());
        String password = new String(passwordField.getPassword());
        if (!password.isEmpty()) {
            editingUser.setPassword(password);
        }
        Packet request = Packet.request(RequestType.USER_UPDATE, editingUser);
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            legacyDepartmentWarning.setText(" "); // 방금 트리에 있는 값으로 저장했으니 경고를 지운다
            JOptionPane.showMessageDialog(this, "저장되었습니다.");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
