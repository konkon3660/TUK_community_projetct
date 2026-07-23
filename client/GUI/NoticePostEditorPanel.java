package client.GUI;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import model.boards.NoticePost;
import model.protocol.BoardKey;
import model.protocol.Packet;
import model.protocol.PostCreateOrUpdateRequest;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 공지 작성/수정 (관리자 전용). 대상 학과(비우면 전체 공지)와 기숙사 공지 여부를 포함한다. */
public class NoticePostEditorPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField titleField = new JTextField();
    private final JTextArea contentArea = new JTextArea();
    private final JTextField targetDepartmentsField = new JTextField(); // 쉼표 구분, 비우면 전체 공지
    private final JCheckBox dormNoticeCheckBox = new JCheckBox("기숙사 공지");
    private final JButton saveButton = new JButton("저장");
    private NoticePost editingPost; // null이면 새 글 작성

    public NoticePostEditorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        saveButton.addActionListener(e -> save());
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** mainFrame.switchTo("noticePostEditor") 전에 반드시 먼저 호출한다. existingPost가 null이면 새 글 작성. */
    public void open(NoticePost existingPost) {
        this.editingPost = existingPost;
        // TODO: 구현 필요. existingPost != null이면 필드들에 기존 값을 채운다.
    }

    private void save() {
        String authorId = mainFrame.getCurrentUser().getId();
        boolean isNew = editingPost == null;
        List<String> targetDepartments = targetDepartmentsField.getText().isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(targetDepartmentsField.getText().split(",")));
        NoticePost post = isNew
                ? new NoticePost(generateId(), titleField.getText(), authorId, contentArea.getText(),
                        null, null, LocalDateTime.now(), targetDepartments, dormNoticeCheckBox.isSelected())
                : editingPost;
        if (!isNew) {
            post.setTitle(titleField.getText());
            post.setContent(contentArea.getText());
        }
        RequestType type = isNew ? RequestType.POST_CREATE : RequestType.POST_UPDATE;
        Packet request = Packet.request(type, new PostCreateOrUpdateRequest(BoardKey.NOTICE, post));
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
