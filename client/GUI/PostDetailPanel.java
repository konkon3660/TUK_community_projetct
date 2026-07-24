package client.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import client.CT.FileTransferClient;
import model.ChatRoom;
import model.User;
import model.boards.Comment;
import model.boards.ComplaintPost;
import model.boards.GroupBuyPost;
import model.boards.NoticePost;
import model.boards.Post;
import model.protocol.ChatRoomJoinRequest;
import model.protocol.CommentAddRequest;
import model.protocol.CommentDeleteRequest;
import model.protocol.FileTransfer;
import model.protocol.Packet;
import model.protocol.PostDeleteRequest;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 게시글 상세 + 댓글. 모든 boardKey/Post 하위타입에서 공용으로 쓴다. */
@SuppressWarnings("unchecked")
public class PostDetailPanel extends JPanel {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MainFrame mainFrame;
    private final JLabel titleLabel = new JLabel();
    private final JLabel metaLabel = new JLabel();
    private final JLabel extraLabel = new JLabel(); // 게시글 타입별 추가 정보 (공동구매 인원/공지 대상 등)
    private final JTextArea contentArea = new JTextArea();
    private final JPanel attachmentPanel = new JPanel();
    private final JButton saveFileButton = new JButton("첨부파일 저장");
    private final JLabel imageLabel = new JLabel();
    private final JLabel commentCountLabel = new JLabel();
    private final DefaultListModel<Comment> commentListModel = new DefaultListModel<>();
    private final JList<Comment> commentJList = new JList<>(commentListModel);
    private final JButton deleteCommentButton = new JButton("댓글 삭제");
    private final JButton editButton = new JButton("수정");
    private final JButton deleteButton = new JButton("삭제");
    private final JButton enterChatButton = new JButton("채팅방 들어가기"); // 공동구매 글 전용
    private final JTextField commentField = new JTextField();
    private final JButton addCommentButton = new JButton("댓글 등록");
    private final JButton backButton = new JButton("뒤로");
    private String boardKey;
    private Post post;
    private ChatRoom linkedRoom; // 공동구매 글일 때만 채워진다

    public PostDetailPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        editButton.addActionListener(e -> openEditor());
        deleteButton.addActionListener(e -> deletePost());
        enterChatButton.addActionListener(e -> enterLinkedRoom());
        addCommentButton.addActionListener(e -> addComment());
        backButton.addActionListener(e -> mainFrame.switchTo("postList"));
        saveFileButton.addActionListener(e -> saveAttachment(post.getFilePath()));
        deleteCommentButton.addActionListener(e -> deleteSelectedComment());
        commentJList.addListSelectionListener(e -> updateCommentDeleteButton());
        initLayout();
    }

    /** 제목/본문/작성자/첨부/댓글 목록 + 위 버튼들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        // 위: 제목 / 작성자·작성시각 / 타입별 정보 / 첨부
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        metaLabel.setFont(metaLabel.getFont().deriveFont(12f));
        metaLabel.setForeground(Color.GRAY);
        extraLabel.setFont(extraLabel.getFont().deriveFont(12f));
        extraLabel.setForeground(Color.GRAY);

        attachmentPanel.setLayout(new BorderLayout(0, 6));
        attachmentPanel.add(saveFileButton, BorderLayout.NORTH);
        attachmentPanel.add(imageLabel, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        JPanel headerBody = new JPanel(new BorderLayout(0, 4));
        headerBody.add(metaLabel, BorderLayout.NORTH);
        headerBody.add(extraLabel, BorderLayout.CENTER);
        headerBody.add(attachmentPanel, BorderLayout.SOUTH);
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(headerBody, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // 가운데: 본문. 읽기 전용이지만 길면 스크롤·선택 복사가 되도록 JTextArea를 쓴다.
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        add(new JScrollPane(contentArea), BorderLayout.CENTER);

        // 아래: 댓글 목록 → 댓글 입력 → 게시글 버튼
        commentJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        commentJList.setCellRenderer(new CommentRenderer());
        commentJList.setFixedCellHeight(24);
        JScrollPane commentScroll = new JScrollPane(commentJList);
        commentScroll.setPreferredSize(new Dimension(0, 140));

        JPanel commentBox = new JPanel(new BorderLayout(0, 4));
        commentBox.add(commentCountLabel, BorderLayout.NORTH);
        commentBox.add(commentScroll, BorderLayout.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.add(commentField, BorderLayout.CENTER);
        JPanel commentButtons = new JPanel();
        commentButtons.add(addCommentButton);
        commentButtons.add(deleteCommentButton);
        inputRow.add(commentButtons, BorderLayout.EAST);

        JPanel buttons = new JPanel();
        buttons.add(enterChatButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(backButton);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(commentBox, BorderLayout.NORTH);
        south.add(inputRow, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        // 로그인 전에 생성되므로 여기서는 getCurrentUser()를 볼 수 없다. 권한에 따른 활성화는
        // renderPost()/updateCommentDeleteButton()이 화면을 열 때 정한다.
        deleteCommentButton.setEnabled(false);
    }

    /** 목록 한 줄의 표시 형식: 작성자 · 작성시각 · 내용 */
    private static class CommentRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            Comment comment = (Comment) value;
            String line = comment.getAuthorId()
                    + "   ·   " + comment.getCreatedAt().format(DISPLAY_TIME)
                    + "   ·   " + comment.getContent();
            return super.getListCellRendererComponent(list, line, index, isSelected, cellHasFocus);
        }
    }

    /** mainFrame.switchTo("postDetail") 전에 반드시 먼저 호출한다. */
    public void open(String boardKey, Post post) {
        this.boardKey = boardKey;
        this.post = post;
        // 공동구매 글의 현재 인원은 연결된 채팅방에서만 알 수 있다. 댓글 등록/삭제 때마다
        // renderPost()가 다시 불리므로, 방은 화면을 열 때 한 번만 받아 둔다.
        this.linkedRoom = post instanceof GroupBuyPost ? findLinkedRoom((GroupBuyPost) post) : null;
        renderPost();
    }

    /** post의 필드/댓글을 화면에 채우고, post.canEdit(mainFrame.getCurrentUser())/canDelete(...)에 따라
     *  editButton/deleteButton 노출 여부를 정하는 부분 — 렌더링은 자유.
     *  첨부는 post.getFilePath()/getImagePath()가 null이 아닐 때만 버튼을 띄우고
     *  saveAttachment(경로) / loadImage(경로)를 연결하면 된다. */
    private void renderPost() {
        User me = mainFrame.getCurrentUser();
        titleLabel.setText(post.getTitle());
        metaLabel.setText(post.getAuthorId() + "   ·   " + post.getCreatedAt().format(DISPLAY_TIME));
        contentArea.setText(post.getContent());
        contentArea.setCaretPosition(0); // 긴 글도 항상 맨 위부터 보이게

        String extra = extraInfo(post);
        extraLabel.setText(extra);
        extraLabel.setVisible(!extra.isEmpty());

        // 버튼을 감추는 건 편의일 뿐이고 실제 권한 판정은 서버가 한다.
        editButton.setVisible(post.canEdit(me));
        deleteButton.setVisible(post.canDelete(me));
        // 채팅방 진입은 공동구매 글이고 연결된 방을 찾았을 때만. 못 찾은 경우(연동 전 예전 글,
        // 목록 조회 실패)에는 눌러봐야 갈 곳이 없으므로 감춘다.
        enterChatButton.setVisible(post instanceof GroupBuyPost && linkedRoom != null);

        // 첨부파일은 버튼만 띄우고 실제 내려받기는 누를 때 한다. 이미지는 바로 보여준다.
        saveFileButton.setVisible(post.getFilePath() != null);
        ImageIcon image = post.getImagePath() == null ? null : loadImage(post.getImagePath());
        imageLabel.setIcon(image);
        imageLabel.setVisible(image != null); // 내려받기에 실패하면 영역을 감춘다
        attachmentPanel.setVisible(saveFileButton.isVisible() || imageLabel.isVisible());

        commentListModel.clear();
        for (Comment comment : post.getComments()) {
            commentListModel.addElement(comment);
        }
        commentCountLabel.setText("댓글 (" + post.getComments().size() + ")");
        commentJList.clearSelection();
        updateCommentDeleteButton();
    }

    /** 게시글 타입별로 제목 아래에 덧붙일 한 줄. 추가 필드가 없는 Post는 빈 문자열. */
    private String extraInfo(Post post) {
        if (post instanceof GroupBuyPost) {
            GroupBuyPost groupBuyPost = (GroupBuyPost) post;
            int current = groupBuyPost.getCurrentMemberCount(linkedRoom);
            String currentText = current == GroupBuyPost.UNKNOWN_MEMBER_COUNT ? "?" : String.valueOf(current);
            String maxText = groupBuyPost.getMaxMembers() == -1 ? "무제한" : groupBuyPost.getMaxMembers() + "명";
            String line = "참여 " + currentText + " / " + maxText;
            if (!groupBuyPost.getHashtags().isEmpty()) {
                line += "   ·   #" + String.join(" #", groupBuyPost.getHashtags());
            }
            return line;
        }
        if (post instanceof NoticePost) {
            NoticePost notice = (NoticePost) post;
            String target = notice.getTargetDepartments().isEmpty()
                    ? "전체 공지"
                    : String.join(", ", notice.getTargetDepartments());
            return notice.isDormNotice() ? target + "   ·   기숙사 공지" : target;
        }
        if (post instanceof ComplaintPost) {
            ComplaintPost complaint = (ComplaintPost) post;
            return complaint.getCategory1() + " > " + complaint.getCategory2()
                    + "   ·   " + (complaint.isAnswered() ? "답변완료" : "답변대기");
        }
        return "";
    }

    /** 공동구매 글에 연결된 채팅방을 찾는다. 방 하나만 받는 요청이 없어서 전체 목록에서 고른다.
     *  아직 참여하지 않은 방도 목록에는 나오므로 인원수는 볼 수 있다. 못 찾으면 null. */
    private ChatRoom findLinkedRoom(GroupBuyPost post) {
        String roomId = post.getChatRoomId();
        if (roomId == null || roomId.isEmpty()) {
            return null; // 채팅방 연동 전에 쓰인 예전 글
        }
        Packet response = mainFrame.getConnection().sendRequest(Packet.request(RequestType.CHATROOM_LIST, null));
        if (response.getStatus() != ResponseStatus.OK) {
            return null; // 인원수는 부가 정보라 실패해도 상세 화면 자체는 열어준다
        }
        for (ChatRoom room : (List<ChatRoom>) response.getPayload()) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }

    /**
     * 공동구매 글에 연결된 채팅방으로 들어간다. 참여자가 아니면 바로 들여보내지 않고 가입 신청을
     * 보낸다 — 참여자가 아니면 서버가 CHAT_SEND를 거부하므로 들어가봐야 아무것도 못 한다
     * (ChatRoomListPanel.openSelectedRoom과 같은 규칙).
     */
    private void enterLinkedRoom() {
        String myId = mainFrame.getCurrentUser().getId();
        if (linkedRoom.getMemberIds().contains(myId)) {
            ((ChatRoomPanel) mainFrame.getScreen("chatRoom")).open(linkedRoom);
            mainFrame.switchTo("chatRoom");
            return;
        }
        // 공동구매 방은 자격 제한이 없어 보통 isOpenJoin이다 — 이 경우 승인 없이 바로 참여시킨다.
        if (linkedRoom.isOpenJoin()) {
            ChatRoom joined = sendJoin("");
            if (joined != null) {
                this.linkedRoom = joined;
                ((ChatRoomPanel) mainFrame.getScreen("chatRoom")).open(joined);
                mainFrame.switchTo("chatRoom");
            }
            return;
        }
        if (linkedRoom.getPendingJoinRequests().containsKey(myId)) {
            JOptionPane.showMessageDialog(this, "이미 가입 신청을 보냈습니다. 방장의 승인을 기다려 주세요.",
                    "가입 신청", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String message = JOptionPane.showInputDialog(this,
                "참여 중이 아닌 채팅방입니다. 가입 신청 메세지를 입력하세요.", "가입 신청", JOptionPane.QUESTION_MESSAGE);
        if (message == null) { // 취소
            return;
        }
        if (sendJoin(message) != null) {
            // 들고 있는 방 사본에도 반영해서 바로 다시 누르면 "이미 신청" 안내가 나오게 한다.
            linkedRoom.getPendingJoinRequests().put(myId, message);
            JOptionPane.showMessageDialog(this, "가입 신청을 보냈습니다. 방장이 승인하면 들어갈 수 있습니다.",
                    "가입 신청", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** 연결된 방에 CHATROOM_JOIN_REQUEST를 보내고 갱신된 방을 반환한다. 실패하면 오류를 띄우고 null. */
    private ChatRoom sendJoin(String message) {
        Packet request = Packet.request(RequestType.CHATROOM_JOIN_REQUEST,
                new ChatRoomJoinRequest(linkedRoom.getRoomId(), message));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() != ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "가입 실패", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return (ChatRoom) response.getPayload();
    }

    /** 첨부를 서버에서 받아 사용자가 고른 위치에 저장한다. 원본 파일명이 기본값으로 채워진다. */
    private void saveAttachment(String storedPath) {
        try {
            FileTransfer attachment = FileTransferClient.download(mainFrame.getConnection(), storedPath);
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(attachment.getFileName()));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            FileTransferClient.saveTo(attachment, chooser.getSelectedFile().toPath());
            JOptionPane.showMessageDialog(this, "저장했습니다: " + chooser.getSelectedFile().getName(),
                    "첨부 저장", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "첨부 내려받기 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 이미지 표시 최대 크기. 이보다 크면 비율을 유지한 채 이 안에 들어오도록 줄인다. */
    private static final int IMAGE_MAX_WIDTH = 560;
    private static final int IMAGE_MAX_HEIGHT = 320;

    /** 첨부 이미지를 화면에 바로 붙일 수 있는 형태로 받아온다. 실패하면 null(= 이미지 영역을 감춘다). */
    private ImageIcon loadImage(String storedPath) {
        try {
            ImageIcon icon = new ImageIcon(
                    FileTransferClient.download(mainFrame.getConnection(), storedPath).getData());
            return fitToScreen(icon);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 원본이 최대 크기를 넘으면 가로/세로 비율을 유지하며 축소한다 (작은 이미지는 그대로). */
    private static ImageIcon fitToScreen(ImageIcon icon) {
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        if (width <= IMAGE_MAX_WIDTH && height <= IMAGE_MAX_HEIGHT) {
            return icon;
        }
        // 가로·세로 중 더 많이 줄여야 하는 쪽에 맞춘다 — 두 축 모두 최대치 안에 들어온다.
        double scale = Math.min((double) IMAGE_MAX_WIDTH / width, (double) IMAGE_MAX_HEIGHT / height);
        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));
        return new ImageIcon(icon.getImage()
                .getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH));
    }

    private void openEditor() {
        // PostListPanel.openEditor()와 같은 갈래지만, 상세 화면은 실제 게시글 객체를 들고 있으므로
        // boardKey가 아니라 post의 타입으로 나눈다 (학과 게시판은 boardKey가 학과명이라 목록만으로는 못 가른다).
        if (post instanceof GroupBuyPost) {
            ((GroupBuyPostEditorPanel) mainFrame.getScreen("groupBuyPostEditor")).open((GroupBuyPost) post);
            mainFrame.switchTo("groupBuyPostEditor");
        } else if (post instanceof NoticePost) {
            ((NoticePostEditorPanel) mainFrame.getScreen("noticePostEditor")).open((NoticePost) post);
            mainFrame.switchTo("noticePostEditor");
        } else {
            // 자유/기숙사/학과 게시판은 물론 민원글도 공용 에디터를 쓴다 — 서버의 POST_UPDATE가
            // 어차피 제목/내용/첨부만 반영하고, ComplaintPanel에는 수정 기능이 없기 때문이다.
            ((PostEditorPanel) mainFrame.getScreen("postEditor")).open(boardKey, post);
            mainFrame.switchTo("postEditor");
        }
    }

    private void deletePost() {
        Packet request = Packet.request(RequestType.POST_DELETE, new PostDeleteRequest(boardKey, post.getId()));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // 목록은 자기가 들고 있는 사본을 그리므로, 새로고침하지 않으면 방금 지운 글이 그대로 보인다.
            ((PostListPanel) mainFrame.getScreen("postList")).refresh();
            mainFrame.switchTo("postList");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "삭제 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addComment() {
        if (commentField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "댓글 내용을 입력하세요.", "댓글 등록", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String authorId = mainFrame.getCurrentUser().getId();
        Comment comment = new Comment(authorId, commentField.getText(), LocalDateTime.now());
        Packet request = Packet.request(RequestType.COMMENT_ADD,
                new CommentAddRequest(boardKey, post.getId(), comment));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // 응답에 payload가 없어서 서버가 저장한 것과 같은 댓글을 들고 있는 사본에도 직접 넣어준다.
            post.addComment(comment);
            commentField.setText("");
            renderPost();
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "댓글 등록 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 목록에서 고른 댓글을 지운다. JList의 위치가 곧 서버가 받는 commentIndex다. */
    private void deleteSelectedComment() {
        int index = commentJList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "삭제할 댓글을 먼저 선택하세요.", "댓글 삭제", JOptionPane.WARNING_MESSAGE);
            return;
        }
        deleteComment(index);
    }

    /** 고른 댓글이 내가 지울 수 있는 것일 때만 삭제 버튼을 켠다 (실제 판정은 서버가 다시 한다). */
    private void updateCommentDeleteButton() {
        User me = mainFrame.getCurrentUser();
        Comment selected = commentJList.getSelectedValue();
        deleteCommentButton.setEnabled(me != null && selected != null && selected.canDelete(me));
    }

    /** commentIndex는 post.getComments() 리스트 내 위치(0부터). */
    private void deleteComment(int commentIndex) {
        Packet request = Packet.request(RequestType.COMMENT_DELETE,
                new CommentDeleteRequest(boardKey, post.getId(), commentIndex));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // 응답에 payload가 없어서 들고 있는 사본에서도 같은 위치를 직접 지운다.
            post.getComments().remove(commentIndex);
            renderPost();
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "댓글 삭제 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
