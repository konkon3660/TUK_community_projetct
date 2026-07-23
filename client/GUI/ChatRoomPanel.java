package client.GUI;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import client.CT.PushListener;
import model.Chat;
import model.ChatRoom;
import model.protocol.ChatPushPayload;
import model.protocol.ChatRoomJoinDecision;
import model.protocol.ChatSendRequest;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 채팅방 화면. 메시지 송수신 + (방장이면) 가입신청 승인/거절. */
public class ChatRoomPanel extends JPanel implements PushListener {
    private final MainFrame mainFrame;
    private final JTextField messageField = new JTextField();
    private final JButton sendButton = new JButton("보내기");
    private final JButton backButton = new JButton("뒤로");
    private ChatRoom room;

    public ChatRoomPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        sendButton.addActionListener(e -> sendMessage());
        backButton.addActionListener(e -> mainFrame.switchTo("chatRoomList"));
        initLayout();
    }

    /** 위 필드들과 채팅 메시지 목록을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** mainFrame.switchTo("chatRoom") 전에 반드시 먼저 호출한다. 이 화면이 떠 있는 동안
     *  실시간 수신을 받도록 ServerConnection의 PushListener를 이 화면으로 등록한다. */
    public void open(ChatRoom room) {
        this.room = room;
        mainFrame.getConnection().setPushListener(this);
        renderMessages();
    }

    private void sendMessage() {
        Packet request = Packet.request(RequestType.CHAT_SEND,
                new ChatSendRequest(room.getRoomId(), messageField.getText()));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "전송 실패", JOptionPane.ERROR_MESSAGE);
        }
        // TODO: 구현 필요. 보낸 메시지를 화면에 바로 반영할지, CHAT_MESSAGE_PUSH로 돌아올 때까지 기다릴지 정한다.
    }

    /** 방장 전용: 가입 신청을 수락한다. userId는 room.getPendingJoinRequests()의 key. */
    private void approveJoin(String userId) {
        Packet request = Packet.request(RequestType.CHATROOM_JOIN_APPROVE,
                new ChatRoomJoinDecision(room.getRoomId(), userId));
        mainFrame.getConnection().sendRequest(request);
        // TODO: 구현 필요. 화면 갱신.
    }

    /** 방장 전용: 가입 신청을 거절한다. */
    private void rejectJoin(String userId) {
        Packet request = Packet.request(RequestType.CHATROOM_JOIN_REJECT,
                new ChatRoomJoinDecision(room.getRoomId(), userId));
        mainFrame.getConnection().sendRequest(request);
        // TODO: 구현 필요. 화면 갱신.
    }

    /** room.getChats()를 그리고, room.getOwnerId()가 나(mainFrame.getCurrentUser())와 같으면
     *  room.getPendingJoinRequests()도 함께 보여주는 부분 — 렌더링은 자유. */
    private void renderMessages() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** 서버가 CHAT_MESSAGE_PUSH를 보낼 때 호출됨(다른 사람이 이 방에 채팅을 보냈을 때). */
    @Override
    public void onPush(Packet packet) {
        if (packet.getType() != RequestType.CHAT_MESSAGE_PUSH) {
            return;
        }
        ChatPushPayload payload = (ChatPushPayload) packet.getPayload();
        if (room == null || !payload.getRoomId().equals(room.getRoomId())) {
            return;
        }
        Chat chat = payload.getChat();
        // TODO: 구현 필요. room.sendChat(chat) 등으로 반영 후 화면 갱신
        //       (이 콜백은 네트워크 스레드에서 호출되므로 SwingUtilities.invokeLater로 감싸야 한다).
    }
}
