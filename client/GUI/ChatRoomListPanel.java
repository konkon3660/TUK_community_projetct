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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
    private final JTextField searchField = new JTextField();
    private final DefaultListModel<ChatRoom> listModel = new DefaultListModel<>();
    private final JList<ChatRoom> roomJList = new JList<>(listModel);
    private List<ChatRoom> rooms;

    public ChatRoomListPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        createButton.addActionListener(e -> mainFrame.switchTo("chatRoomCreate"));
        refreshButton.addActionListener(e -> refresh());
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        enterButton.addActionListener(e -> openSelectedRoom());
        // 타이핑하는 즉시 걸러지도록 — 별도 검색 버튼 없이 DocumentListener로 실시간 필터링.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { renderRooms(); }
            @Override
            public void removeUpdate(DocumentEvent e) { renderRooms(); }
            @Override
            public void changedUpdate(DocumentEvent e) { renderRooms(); }
        });
        initLayout();
    }

    /** 위 버튼들과 채팅방 목록을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        // PostListPanel과 같은 구성: 위에 제목, 가운데 목록, 아래 버튼 줄.
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        JPanel header = new JPanel(new BorderLayout(0, 6));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 20f));
        header.add(headerLabel, BorderLayout.NORTH);
        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.add(new JLabel("검색"), BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        header.add(searchRow, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

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
            line.append('[').append(room.getRoomId()).append("] ");
            if (!room.getName().isEmpty()) {
                line.append(room.getName()).append("   ·   ");
            }
            line.append("방장 ").append(room.getOwnerId())
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

    /** rooms를 검색어로 거른 뒤 화면에 그리고, 항목을 클릭하면
     *  ((ChatRoomPanel) mainFrame.getScreen("chatRoom")).open(room) 후 switchTo("chatRoom") — 렌더링은 자유. */
    private void renderRooms() {
        if (rooms == null) {
            return; // refresh()로 아직 한 번도 받아오지 않은 상태
        }
        String query = searchField.getText().trim().toLowerCase();
        listModel.clear();
        int shown = 0;
        for (ChatRoom room : rooms) {
            if (!matchesSearch(room, query)) {
                continue;
            }
            listModel.addElement(room);
            shown++;
        }
        headerLabel.setText("채팅방 (" + shown + "/" + rooms.size() + ")");
        roomJList.clearSelection();
    }

    /** 방 이름 · 방번호 · 방장 학번 중 하나라도 검색어를 포함하면 매치 (대소문자 무시). 빈 검색어는 전체 매치. */
    private boolean matchesSearch(ChatRoom room, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return room.getName().toLowerCase().contains(query)
                || room.getRoomId().toLowerCase().contains(query)
                || room.getOwnerId().toLowerCase().contains(query);
    }

    /** 선택한 방으로 들어간다. 아직 참여자가 아니면 먼저 가입 절차를 탄다
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
        enter(selected);
    }

    /** 실제로 채팅방 화면을 연다. */
    private void enter(ChatRoom room) {
        ((ChatRoomPanel) mainFrame.getScreen("chatRoom")).open(room);
        mainFrame.switchTo("chatRoom");
    }

    /**
     * 가입 절차. 자격 제한(입학년도/학과/기숙사)이 없는 방은 승인 없이 바로 입장하고,
     * 제한이 있는 방만 가입지원 메세지를 받아 신청한다(승인은 방장이 채팅방 화면에서 한다).
     */
    private void requestJoin(ChatRoom room) {
        if (room.isOpenJoin()) {
            ChatRoom joined = sendJoin(room, ""); // 제한 없는 방 — 메세지 없이 즉시 참여
            if (joined != null) {
                enter(joined); // 서버가 참여자로 넣어준 방으로 바로 입장
            }
            return;
        }
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
        if (sendJoin(room, message) != null) {
            JOptionPane.showMessageDialog(this, "가입 신청을 보냈습니다. 방장이 승인하면 들어갈 수 있습니다.",
                    "가입 신청", JOptionPane.INFORMATION_MESSAGE);
            refresh(); // 방금 보낸 신청이 "승인 대기중"으로 보이도록
        }
    }

    /** CHATROOM_JOIN_REQUEST를 보내고 서버가 돌려준 갱신된 방을 반환한다. 실패하면 오류를 띄우고 null. */
    private ChatRoom sendJoin(ChatRoom room, String message) {
        Packet request = Packet.request(RequestType.CHATROOM_JOIN_REQUEST,
                new ChatRoomJoinRequest(room.getRoomId(), message));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "가입 실패", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return (ChatRoom) response.getPayload();
    }

    private boolean isMember(ChatRoom room) {
        return room.getMemberIds().contains(myId());
    }

    private String myId() {
        return mainFrame.getCurrentUser().getId();
    }
}
