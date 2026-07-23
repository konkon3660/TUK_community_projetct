package client.GUI;

import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import model.ChatRoom;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 전체 채팅방 탐색/검색 화면. CHATROOM_LIST로 목록을 받아온다. */
@SuppressWarnings("unchecked")
public class ChatRoomListPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JButton createButton = new JButton("채팅방 만들기");
    private final JButton refreshButton = new JButton("새로고침");
    private final JButton backButton = new JButton("뒤로");
    private List<ChatRoom> rooms;

    public ChatRoomListPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        createButton.addActionListener(e -> mainFrame.switchTo("chatRoomCreate"));
        refreshButton.addActionListener(e -> refresh());
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        initLayout();
    }

    /** 위 버튼들과 채팅방 목록을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** mainFrame.switchTo("chatRoomList") 직후(혹은 화면이 다시 보일 때마다) 호출해서 목록을 갱신한다. */
    public void refresh() {
        Packet request = Packet.request(RequestType.CHATROOM_LIST, null);
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            this.rooms = (List<ChatRoom>) response.getPayload();
            renderRooms();
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "채팅방 목록 조회 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** rooms를 화면에 그리고, 항목을 클릭하면
     *  ((ChatRoomPanel) mainFrame.getScreen("chatRoom")).open(room) 후 switchTo("chatRoom") — 렌더링은 자유. */
    private void renderRooms() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
