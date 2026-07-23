package client.GUI;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import model.ChatRoom;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 채팅방 생성 폼(정원/기숙사 제한/학과 제한). */
public class ChatRoomCreatePanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField maxMembersField = new JTextField();
    private final JCheckBox dormOnlyCheckBox = new JCheckBox("기숙사생만");
    private final JTextField departmentLimitField = new JTextField(); // 쉼표 구분, 비우면 학과 제한 없음
    private final JButton createButton = new JButton("만들기");
    private final JButton backButton = new JButton("뒤로");

    public ChatRoomCreatePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        createButton.addActionListener(e -> create());
        backButton.addActionListener(e -> mainFrame.switchTo("chatRoomList"));
        // 입력칸에서 엔터를 쳐도 만들어지게 한다 (RegisterPanel과 동일한 조작감).
        maxMembersField.addActionListener(e -> create());
        departmentLimitField.addActionListener(e -> create());
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        // RegisterPanel과 같은 구성: GridBagLayout 위에 폼 하나만 올려 항상 가운데 정렬.
        setLayout(new GridBagLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints c = new GridBagConstraints();

        JLabel title = new JLabel("채팅방 만들기");
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
        form.add(new JLabel("정원"), c);
        c.gridy = 2;
        form.add(new JLabel("학과 제한"), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        maxMembersField.setColumns(16);
        departmentLimitField.setColumns(16);
        c.gridy = 1;
        form.add(maxMembersField, c);
        c.gridy = 2;
        form.add(departmentLimitField, c);

        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 6, 0, 6);
        form.add(dormOnlyCheckBox, c);

        JLabel hint = new JLabel("※ 정원은 숫자, -1이면 무제한 · 학과 제한은 쉼표로 구분, 비우면 제한 없음");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(Color.GRAY);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 6, 0, 6);
        form.add(hint, c);

        JPanel buttons = new JPanel();
        buttons.add(createButton);
        buttons.add(backButton);
        c.gridy = 5;
        c.insets = new Insets(14, 6, 0, 6);
        form.add(buttons, c);

        add(form, new GridBagConstraints());
    }

    private void create() {
        String ownerId = mainFrame.getCurrentUser().getId();
        int maxMembers;
        try {
            maxMembers = Integer.parseInt(maxMembersField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "정원은 숫자로 입력하세요 (-1이면 무제한).",
                    "채팅방 생성 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // roomId는 서버가 채번해서 응답으로 돌려준다 — 요청에는 빈 문자열로 보낸다.
        ChatRoom newRoom = new ChatRoom("", ownerId, maxMembers);
        newRoom.setDormOnlyLimit(dormOnlyCheckBox.isSelected());
        if (!departmentLimitField.getText().isEmpty()) {
            newRoom.getDepartmentLimit().addAll(Arrays.asList(departmentLimitField.getText().split(",")));
        }
        Packet request = Packet.request(RequestType.CHATROOM_CREATE, newRoom);
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // 방장은 서버가 바로 참여자로 넣어주므로(ClientHandler.handleChatRoomCreate)
            // 만든 방에 그대로 들어간다. 응답으로 온 방에는 서버가 채번한 roomId가 들어있다.
            ChatRoom created = (ChatRoom) response.getPayload();
            clearFields(); // 다음에 이 화면으로 다시 들어왔을 때 이전 입력이 남아있지 않도록
            ((ChatRoomPanel) mainFrame.getScreen("chatRoom")).open(created);
            mainFrame.switchTo("chatRoom");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "채팅방 생성 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        maxMembersField.setText("");
        departmentLimitField.setText("");
        dormOnlyCheckBox.setSelected(false);
    }
}
