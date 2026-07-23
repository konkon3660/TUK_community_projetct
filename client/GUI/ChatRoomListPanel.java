package client.GUI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.ListSelectionModel;

import model.ChatRoom;
import model.protocol.ChatRoomJoinRequest;
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
    private final JButton enterButton = new JButton("들어가기");
    private final JLabel headerLabel = new JLabel();
    private final DefaultListModel<ChatRoom> listModel = new DefaultListModel<>();
    private final JList<ChatRoom> roomJList = new JList<>(listModel);
    private List<ChatRoom> rooms;

    public ChatRoomListPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        createButton.addActionListener(e -> mainFrame.switchTo("chatRoomCreate"));
        refreshButton.addActionListener(e -> refresh());
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        enterButton.addActionListener(e -> openSelectedRoom());
        initLayout();
    }

    /** 위 버튼들과 채팅방 목록을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        // PostListPanel과 같은 구성: 위에 제목, 가운데 목록, 아래 버튼 줄.
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 20f));
        add(headerLabel, BorderLayout.NORTH);

        roomJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomJList.setCellRenderer(new ChatRoomRenderer());
        roomJList.setFixedCellHeight(28);
        roomJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 더블클릭으로도 들어갈 수 있게
                    openSelectedRoom();
                }
            }
        });
        add(new JScrollPane(roomJList), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.add(enterButton);
        buttons.add(createButton);
        buttons.add(refreshButton);
        buttons.add(backButton);
        add(buttons, BorderLayout.SOUTH);
    }

    /** 목록 한 줄의 표시 형식: [방번호] 방장 · 인원 · 가입 제한 · 내 참여 여부 */
    private class ChatRoomRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            ChatRoom room = (ChatRoom) value;
            StringBuilder line = new StringBuilder();
            line.append('[').append(room.getRoomId()).append("]   방장 ").append(room.getOwnerId())
                    .append("   ·   인원 ").append(room.getMemberIds().size())
                    .append('/').append(room.getMaxMembers() == -1 ? "무제한" : room.getMaxMembers());
            if (room.getAdmissionYearLimit() != null) {
                line.append("   ·   ").append(room.getAdmissionYearLimit()).append("학번만");
            }
            if (!room.getDepartmentLimit().isEmpty()) {
                line.append("   ·   ").append(String.join(",", room.getDepartmentLimit()));
            }
            if (room.isDormOnlyLimit()) {
                line.append("   ·   기숙사생만");
            }
            if (isMember(room)) {
                line.append("   ·   참여중");
            } else if (room.getPendingJoinRequests().containsKey(myId())) {
                line.append("   ·   승인 대기중");
            }
            return super.getListCellRendererComponent(list, line.toString(), index, isSelected, cellHasFocus);
        }
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
        headerLabel.setText("채팅방 (" + rooms.size() + ")");
        listModel.clear();
        for (ChatRoom room : rooms) {
            listModel.addElement(room);
        }
        roomJList.clearSelection();
    }

    /** 선택한 방으로 들어간다. 아직 참여자가 아니면 먼저 가입 신청을 보낸다
     *  (참여자가 아니면 서버가 CHAT_SEND를 거부하므로 그냥 들여보내면 아무것도 못 한다). */
    private void openSelectedRoom() {
        ChatRoom selected = roomJList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "채팅방을 먼저 선택하세요.", "들어가기", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!isMember(selected)) {
            requestJoin(selected);
            return;
        }
        ((ChatRoomPanel) mainFrame.getScreen("chatRoom")).open(selected);
        mainFrame.switchTo("chatRoom");
    }

    /** 가입지원 메세지를 받아 CHATROOM_JOIN_REQUEST를 보낸다. 승인은 방장이 채팅방 화면에서 한다. */
    private void requestJoin(ChatRoom room) {
        if (room.getPendingJoinRequests().containsKey(myId())) {
            JOptionPane.showMessageDialog(this, "이미 가입 신청을 보냈습니다. 방장의 승인을 기다려 주세요.",
                    "가입 신청", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String message = JOptionPane.showInputDialog(this,
                "참여 중이 아닌 채팅방입니다. 가입 신청 메세지를 입력하세요.", "가입 신청", JOptionPane.QUESTION_MESSAGE);
        if (message == null) { // 취소
            return;
        }
        Packet request = Packet.request(RequestType.CHATROOM_JOIN_REQUEST,
                new ChatRoomJoinRequest(room.getRoomId(), message));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, "가입 신청을 보냈습니다. 방장이 승인하면 들어갈 수 있습니다.",
                    "가입 신청", JOptionPane.INFORMATION_MESSAGE);
            refresh(); // 방금 보낸 신청이 "승인 대기중"으로 보이도록
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "가입 신청 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isMember(ChatRoom room) {
        return room.getMemberIds().contains(myId());
    }

    private String myId() {
        return mainFrame.getCurrentUser().getId();
    }
}
