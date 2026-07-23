package client.GUI;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import model.boards.GroupBuyPost;
import model.protocol.BoardKey;
import model.protocol.Packet;
import model.protocol.PostCreateOrUpdateRequest;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 공동구매 게시글 작성/수정 (최대인원/해시태그 등 GroupBuyPost 전용 필드 포함). */
public class GroupBuyPostEditorPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField titleField = new JTextField();
    private final JTextArea contentArea = new JTextArea();
    private final JTextField maxMembersField = new JTextField();
    private final JTextField hashtagsField = new JTextField(); // 쉼표로 구분해 입력
    private final AttachmentPicker attachmentPicker;
    private final JButton saveButton = new JButton("저장");
    private GroupBuyPost editingPost; // null이면 새 글 작성

    public GroupBuyPostEditorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.attachmentPicker = new AttachmentPicker(mainFrame, true);
        saveButton.addActionListener(e -> save());
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유.
     *  (attachmentPicker는 그 자체가 JPanel이라 add(attachmentPicker) 한 줄이면 된다.) */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** mainFrame.switchTo("groupBuyPostEditor") 전에 반드시 먼저 호출한다. existingPost가 null이면 새 글 작성. */
    public void open(GroupBuyPost existingPost) {
        this.editingPost = existingPost;
        attachmentPicker.reset(
                existingPost == null ? null : existingPost.getFilePath(),
                existingPost == null ? null : existingPost.getImagePath());
        // TODO: 구현 필요. existingPost != null이면 필드들에 기존 값을 채운다.
    }

    private void save() {
        String authorId = mainFrame.getCurrentUser().getId();
        boolean isNew = editingPost == null;
        GroupBuyPost post;
        if (isNew) {
            List<String> hashtags = new ArrayList<>(Arrays.asList(hashtagsField.getText().split(",")));
            // chatRoomId는 null로 두면 된다 — 서버가 POST_CREATE 안에서 채팅방을 만들어 채운 뒤
            // 그 게시글을 응답으로 돌려준다 (CHATROOM_CREATE를 따로 보내지 말 것).
            post = new GroupBuyPost(generateId(), titleField.getText(), authorId, contentArea.getText(),
                    attachmentPicker.getFilePath(), attachmentPicker.getImagePath(), LocalDateTime.now(),
                    Integer.parseInt(maxMembersField.getText()), null, hashtags);
        } else {
            post = editingPost;
            post.setTitle(titleField.getText());
            post.setContent(contentArea.getText());
            post.setMaxMembers(Integer.parseInt(maxMembersField.getText()));
            // 첨부는 이미 FILE_UPLOAD로 올라가 있고, 여기서는 그 경로만 게시글에 반영한다.
            post.setFilePath(attachmentPicker.getFilePath());
            post.setImagePath(attachmentPicker.getImagePath());
        }
        RequestType type = isNew ? RequestType.POST_CREATE : RequestType.POST_UPDATE;
        Packet request = Packet.request(type, new PostCreateOrUpdateRequest(BoardKey.GROUP_BUY, post));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // TODO: 구현 필요. 예: mainFrame.switchTo("postList");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 새 글의 id를 어떻게 채번할지 아직 팀 논의가 없었다(UUID vs 다른 규칙). */
    private String generateId() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
