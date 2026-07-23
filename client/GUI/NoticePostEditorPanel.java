package client.GUI;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    // 단과대/학부 단위로 "모두"를 골라 그 밑 전체 학과에 한 번에 보낼 수 있다 (getTargetDepartments()).
    private final DepartmentPickerPanel targetDepartmentPicker = new DepartmentPickerPanel(true);
    private final JCheckBox dormNoticeCheckBox = new JCheckBox("기숙사 공지");
    private final AttachmentPicker attachmentPicker;
    private final JButton saveButton = new JButton("저장");
    private final JButton cancelButton = new JButton("취소");
    private final JLabel titleLabel = new JLabel("공지 작성");
    private NoticePost editingPost; // null이면 새 글 작성

    public NoticePostEditorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.attachmentPicker = new AttachmentPicker(mainFrame, true);
        saveButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> mainFrame.switchTo("admin"));
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        add(titleLabel, BorderLayout.NORTH);

        // 본문(JTextArea)만 남는 공간을 전부 쓰고, 나머지 한 줄짜리 입력들은 위아래로 붙인다.
        JPanel head = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 0;
        c.gridy = 0;
        head.add(new JLabel("제목"), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        head.add(titleField, c);

        JPanel body = new JPanel(new BorderLayout(0, 6));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        body.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        JPanel foot = new JPanel(new GridBagLayout());
        GridBagConstraints f = new GridBagConstraints();
        f.insets = new Insets(4, 4, 4, 4);
        f.anchor = GridBagConstraints.LINE_END;
        f.gridx = 0;
        f.gridy = 0;
        f.gridwidth = 2;
        f.weightx = 0;
        f.fill = GridBagConstraints.NONE;
        f.anchor = GridBagConstraints.LINE_START;
        foot.add(targetDepartmentPicker, f);

        f.gridy = 1;
        f.gridwidth = 1;
        f.anchor = GridBagConstraints.LINE_END;
        f.insets = new Insets(0, 4, 4, 4);
        foot.add(dormNoticeCheckBox, f);

        f.gridx = 0;
        f.gridy = 3;
        f.gridwidth = 2;
        foot.add(attachmentPicker, f);

        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(cancelButton);
        f.gridx = 0;
        f.gridy = 4;
        f.gridwidth = 2;
        f.anchor = GridBagConstraints.CENTER;
        f.insets = new Insets(10, 4, 0, 4);
        foot.add(buttons, f);

        body.add(head, BorderLayout.NORTH);
        body.add(foot, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    /** mainFrame.switchTo("noticePostEditor") 전에 반드시 먼저 호출한다. existingPost가 null이면 새 글 작성. */
    public void open(NoticePost existingPost) {
        this.editingPost = existingPost;
        if (existingPost == null) {
            // 새 글: 이전에 열어둔 공지의 값이 남아있지 않도록 전부 비운다.
            titleLabel.setText("공지 작성");
            titleField.setText("");
            contentArea.setText("");
            dormNoticeCheckBox.setSelected(false);
            attachmentPicker.reset(null, null);
        } else {
            titleLabel.setText("공지 수정");
            titleField.setText(existingPost.getTitle());
            contentArea.setText(existingPost.getContent());
            dormNoticeCheckBox.setSelected(existingPost.isDormNotice());
            attachmentPicker.reset(existingPost.getFilePath(), existingPost.getImagePath());
        }
        // 수정할 때는 대상 범위를 바꿀 수 없어서 아예 비활성화한다:
        // dormNotice는 NoticePost에서 final이고, 서버의 POST_UPDATE도 제목/내용/첨부만 반영한다.
        // 대상 범위를 바꾸려면 공지를 지우고 다시 작성해야 한다. (기존 대상 학과를 픽커에
        // 그대로 되비추지는 않는다 — 여러 학과일 수 있는데 픽커는 한 조합만 표시할 수 있고,
        // 어차피 비활성화라 편집에 쓰이지도 않는다.)
        targetDepartmentPicker.setEnabled(existingPost == null);
        dormNoticeCheckBox.setEnabled(existingPost == null);
    }

    private void save() {
        String authorId = mainFrame.getCurrentUser().getId();
        boolean isNew = editingPost == null;
        List<String> targetDepartments = targetDepartmentPicker.getTargetDepartments();
        NoticePost post = isNew
                ? new NoticePost(generateId(), titleField.getText(), authorId, contentArea.getText(),
                        attachmentPicker.getFilePath(), attachmentPicker.getImagePath(),
                        LocalDateTime.now(), targetDepartments, dormNoticeCheckBox.isSelected())
                : editingPost;
        if (!isNew) {
            post.setTitle(titleField.getText());
            post.setContent(contentArea.getText());
            // 첨부는 이미 FILE_UPLOAD로 올라가 있고, 여기서는 그 경로만 게시글에 반영한다.
            post.setFilePath(attachmentPicker.getFilePath());
            post.setImagePath(attachmentPicker.getImagePath());
        }
        RequestType type = isNew ? RequestType.POST_CREATE : RequestType.POST_UPDATE;
        Packet request = Packet.request(type, new PostCreateOrUpdateRequest(BoardKey.NOTICE, post));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // 저장 결과를 바로 확인할 수 있게 공지 목록으로 보낸다.
            // 관리자가 열었으므로 뒤로가기 대상은 학생 화면이 아니라 "admin".
            ((PostListPanel) mainFrame.getScreen("postList")).open(BoardKey.NOTICE, "admin");
            mainFrame.switchTo("postList");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 게시글 id 채번 규칙. 여러 클라이언트가 각자 만들어도 겹치지 않아야 해서 UUID를 쓴다
     * (서버는 중복 id를 거부하므로 겹치면 저장 자체가 실패한다).
     * 다른 에디터(PostEditorPanel/GroupBuyPostEditorPanel/ComplaintPanel)도 같은 규칙으로 통일할 것.
     */
    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
