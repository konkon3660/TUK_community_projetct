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

/** 관리자 전용: 학번으로 회원을 찾아 학과/기숙사 여부/비밀번호를 수정한다. */
public class UserEditPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField searchIdField = new JTextField();
    private final JButton searchButton = new JButton("조회");
    private final JTextField departmentField = new JTextField();
    private final JCheckBox dormitoryCheckBox = new JCheckBox("기숙사생");
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton saveButton = new JButton("저장");
    private User editingUser;

    public UserEditPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        searchButton.addActionListener(e -> search());
        saveButton.addActionListener(e -> save());
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** searchIdField에 입력한 학번으로 회원 1명을 조회하는 요청이 현재 프로토콜에 없다
     *  (POST_LIST처럼 "학번으로 유저 1명 조회"용 RequestType이 아직 없음 — 필요하면
     *  documents/protocol.md에 새 RequestType을 추가하고 함께 갱신할 것). */
    private void search() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void save() {
        editingUser.setDepartment(departmentField.getText());
        editingUser.setDormitory(dormitoryCheckBox.isSelected());
        String password = new String(passwordField.getPassword());
        if (!password.isEmpty()) {
            editingUser.setPassword(password);
        }
        Packet request = Packet.request(RequestType.USER_UPDATE, editingUser);
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, "저장되었습니다.");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
