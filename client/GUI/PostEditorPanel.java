package client.GUI;

import java.time.LocalDateTime;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import model.boards.Post;
import model.protocol.Packet;
import model.protocol.PostCreateOrUpdateRequest;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 자유/학과/기숙사 게시판처럼 추가 필드가 없는 Post를 작성/수정하는 화면. */
public class PostEditorPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField titleField = new JTextField();
    private final JTextArea contentArea = new JTextArea();
    private final JButton saveButton = new JButton("저장");
    private String boardKey;
    private Post editingPost; // null이면 새 글 작성

    public PostEditorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        saveButton.addActionListener(e -> save());
        initLayout();
    }

    /** titleField/contentArea/saveButton을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** mainFrame.switchTo("postEditor") 전에 반드시 먼저 호출한다. existingPost가 null이면 새 글 작성. */
    public void open(String boardKey, Post existingPost) {
        this.boardKey = boardKey;
        this.editingPost = existingPost;
        // TODO: 구현 필요. existingPost != null이면 titleField/contentArea에 기존 값을 채운다.
    }

    private void save() {
        String authorId = mainFrame.getCurrentUser().getId();
        boolean isNew = editingPost == null;
        Post post = isNew
                ? new Post(generateId(), titleField.getText(), authorId, contentArea.getText(), null, null, LocalDateTime.now())
                : editingPost;
        if (!isNew) {
            post.setTitle(titleField.getText());
            post.setContent(contentArea.getText());
        }
        RequestType type = isNew ? RequestType.POST_CREATE : RequestType.POST_UPDATE;
        Packet request = Packet.request(type, new PostCreateOrUpdateRequest(boardKey, post));
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
