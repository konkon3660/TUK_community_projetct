package client.GUI;

import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

    public ChatRoomCreatePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        createButton.addActionListener(e -> create());
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void create() {
        String ownerId = mainFrame.getCurrentUser().getId();
        int maxMembers = Integer.parseInt(maxMembersField.getText());
        // roomId는 서버가 채번해서 응답으로 돌려준다 — 요청에는 빈 문자열로 보낸다.
        ChatRoom newRoom = new ChatRoom("", ownerId, maxMembers);
        newRoom.setDormOnlyLimit(dormOnlyCheckBox.isSelected());
        if (!departmentLimitField.getText().isEmpty()) {
            newRoom.getDepartmentLimit().addAll(Arrays.asList(departmentLimitField.getText().split(",")));
        }
        Packet request = Packet.request(RequestType.CHATROOM_CREATE, newRoom);
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // TODO: 구현 필요. 예: ChatRoom created = (ChatRoom) response.getPayload();
            //       ((ChatRoomPanel) mainFrame.getScreen("chatRoom")).open(created); mainFrame.switchTo("chatRoom");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "채팅방 생성 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
