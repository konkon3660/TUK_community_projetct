package client.GUI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

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
@SuppressWarnings("unchecked")
public class ChatRoomPanel extends JPanel implements PushListener {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final MainFrame mainFrame;
    private final JTextField messageField = new JTextField();
    private final JButton sendButton = new JButton("보내기");
    private final JButton backButton = new JButton("뒤로");
    private final JButton refreshButton = new JButton("새로고침");
    private final JLabel headerLabel = new JLabel();
    private final JTextArea chatArea = new JTextArea();
    private final JPanel pendingPanel = new JPanel(new BorderLayout(0, 6));
    private final DefaultListModel<String> pendingModel = new DefaultListModel<>();
    private final JList<String> pendingJList = new JList<>(pendingModel);
    private final JButton approveButton = new JButton("승인");
    private final JButton rejectButton = new JButton("거절");
    private ChatRoom room;

    public ChatRoomPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        sendButton.addActionListener(e -> sendMessage());
        backButton.addActionListener(e -> mainFrame.switchTo("chatRoomList"));
        refreshButton.addActionListener(e -> refresh());
        approveButton.addActionListener(e -> approveSelectedJoin());
        rejectButton.addActionListener(e -> rejectSelectedJoin());
        // 입력칸에서 엔터를 쳐도 전송되게 한다.
        messageField.addActionListener(e -> sendMessage());
        initLayout();
    }

    /** 위 필드들과 채팅 메시지 목록을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 20f));
        add(headerLabel, BorderLayout.NORTH);

        chatArea.setEditable(false); // 읽기 전용 — 입력은 아래 messageField로만
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // 가입 신청 목록은 방장에게만 보인다 (renderPendingJoinRequests에서 켜고 끈다).
        JLabel pendingTitle = new JLabel("가입 신청");
        pendingTitle.setFont(pendingTitle.getFont().deriveFont(Font.BOLD, 13f));
        pendingJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pendingJList.setCellRenderer(new PendingRenderer());
        JScrollPane pendingScroll = new JScrollPane(pendingJList);
        pendingScroll.setPreferredSize(new Dimension(220, 0));
        JPanel pendingButtons = new JPanel();
        pendingButtons.add(approveButton);
        pendingButtons.add(rejectButton);
        pendingPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        pendingPanel.add(pendingTitle, BorderLayout.NORTH);
        pendingPanel.add(pendingScroll, BorderLayout.CENTER);
        pendingPanel.add(pendingButtons, BorderLayout.SOUTH);
        pendingPanel.setVisible(false);
        add(pendingPanel, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        JPanel bottomButtons = new JPanel();
        bottomButtons.add(sendButton);
        bottomButtons.add(refreshButton);
        bottomButtons.add(backButton);
        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(bottomButtons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    /** 가입 신청 한 줄의 표시 형식: 학번 — 가입지원 메세지 */
    private class PendingRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            String userId = (String) value;
            String message = room == null ? null : room.getPendingJoinRequests().get(userId);
            String line = (message == null || message.isEmpty()) ? userId : userId + " — " + message;
            return super.getListCellRendererComponent(list, line, index, isSelected, cellHasFocus);
        }
    }

    /** mainFrame.switchTo("chatRoom") 전에 반드시 먼저 호출한다. 이 화면이 떠 있는 동안
     *  실시간 수신을 받도록 ServerConnection의 PushListener를 이 화면으로 등록한다. */
    public void open(ChatRoom room) {
        this.room = room;
        mainFrame.getConnection().setPushListener(this);
        renderMessages();
    }

    /**
     * 서버에서 이 방의 최신 상태를 다시 받아온다. 들고 있는 room은 채팅방 목록을 받아온 시점의
     * 사본이라, 그 뒤에 들어온 가입 신청이나 내가 접속하지 않았던 동안의 채팅은 들어있지 않다.
     * 방 하나만 가져오는 요청이 없으므로 CHATROOM_LIST에서 같은 roomId를 찾아 쓴다.
     */
    private void refresh() {
        if (room == null) {
            return;
        }
        Packet response = mainFrame.getConnection().sendRequest(Packet.request(RequestType.CHATROOM_LIST, null));
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "채팅방 조회 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (ChatRoom candidate : (List<ChatRoom>) response.getPayload()) {
            if (candidate.getRoomId().equals(room.getRoomId())) {
                this.room = candidate;
                renderMessages();
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "채팅방이 사라졌습니다: " + room.getRoomId(),
                "채팅방 조회 실패", JOptionPane.ERROR_MESSAGE);
    }

    private void sendMessage() {
        String content = messageField.getText().trim();
        if (content.isEmpty()) {
            return;
        }
        Packet request = Packet.request(RequestType.CHAT_SEND,
                new ChatSendRequest(room.getRoomId(), content));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "전송 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 서버는 보낸 사람에게는 CHAT_MESSAGE_PUSH를 보내지 않는다(ClientHandler.handleChatSend).
        // 기다려도 오지 않으므로 내가 보낸 메시지는 여기서 직접 화면에 넣는다.
        // 시각은 서버가 찍은 것과 몇 ms 다를 수 있지만, 다시 들어오면 서버 값으로 맞춰진다.
        room.sendChat(new Chat(myId(), LocalDateTime.now(), content));
        messageField.setText("");
        renderMessages();
    }

    /** 방장 전용: 가입 신청을 수락한다. userId는 room.getPendingJoinRequests()의 key. */
    private void approveJoin(String userId) {
        Packet request = Packet.request(RequestType.CHATROOM_JOIN_APPROVE,
                new ChatRoomJoinDecision(room.getRoomId(), userId));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "가입 승인 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        room.approveJoin(userId); // 서버가 한 것과 같은 처리를 들고 있는 사본에도 반영
        renderMessages();
    }

    /** 방장 전용: 가입 신청을 거절한다. */
    private void rejectJoin(String userId) {
        Packet request = Packet.request(RequestType.CHATROOM_JOIN_REJECT,
                new ChatRoomJoinDecision(room.getRoomId(), userId));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "가입 거절 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        room.rejectJoin(userId);
        renderMessages();
    }

    private void approveSelectedJoin() {
        String userId = selectedPendingUserId();
        if (userId != null) {
            approveJoin(userId);
        }
    }

    private void rejectSelectedJoin() {
        String userId = selectedPendingUserId();
        if (userId != null) {
            rejectJoin(userId);
        }
    }

    private String selectedPendingUserId() {
        String selected = pendingJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "가입 신청을 먼저 선택하세요.", "가입 신청", JOptionPane.WARNING_MESSAGE);
        }
        return selected;
    }

    /** room.getChats()를 그리고, room.getOwnerId()가 나(mainFrame.getCurrentUser())와 같으면
     *  room.getPendingJoinRequests()도 함께 보여주는 부분 — 렌더링은 자유. */
    private void renderMessages() {
        if (room == null) {
            return;
        }
        headerLabel.setText("채팅방 " + room.getRoomId()
                + "   ·   인원 " + room.getMemberIds().size()
                + "/" + (room.getMaxMembers() == -1 ? "무제한" : room.getMaxMembers())
                + (isOwner() ? "   ·   내가 방장" : ""));

        StringBuilder text = new StringBuilder();
        for (Chat chat : room.getChats()) {
            text.append('[').append(chat.getSentAt().format(DISPLAY_TIME)).append("] ")
                    .append(displayName(chat.getSenderId())).append(": ")
                    .append(chat.getContent()).append('\n');
        }
        chatArea.setText(text.toString());
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // 항상 최신 메시지가 보이도록

        renderPendingJoinRequests();
    }

    /** 가입 신청은 방장만 처리할 수 있으므로(서버도 방장만 허용) 방장에게만 보여준다. */
    private void renderPendingJoinRequests() {
        boolean owner = isOwner();
        pendingModel.clear();
        if (owner) {
            for (String userId : room.getPendingJoinRequests().keySet()) {
                pendingModel.addElement(userId);
            }
        }
        pendingPanel.setVisible(owner);
        revalidate();
        repaint();
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
        // 이 콜백은 네트워크 스레드(ServerConnection의 readLoop)에서 호출된다. Swing 컴포넌트는
        // 이벤트 디스패치 스레드에서만 만져야 하므로 반영은 invokeLater 안에서 한다.
        SwingUtilities.invokeLater(() -> {
            // 큐에 들어가 있는 동안 다른 방으로 옮겨갔을 수 있으므로 여기서 한 번 더 확인한다.
            if (room != null && payload.getRoomId().equals(room.getRoomId())) {
                room.sendChat(chat);
                renderMessages();
            }
        });
    }

    private boolean isOwner() {
        return room != null && mainFrame.getCurrentUser() != null
                && room.getOwnerId().equals(myId());
    }

    /** 방 안에서 쓰는 프로필 이름이 있으면 그것을, 없으면 학번을 그대로 보여준다. */
    private String displayName(String userId) {
        String nickname = room.getNickname(userId);
        String name = (nickname == null || nickname.isEmpty()) ? userId : nickname;
        return userId.equals(myId()) ? name + " (나)" : name;
    }

    private String myId() {
        return mainFrame.getCurrentUser().getId();
    }
}
