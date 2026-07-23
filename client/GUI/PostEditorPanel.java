package client.GUI;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    private final AttachmentPicker attachmentPicker;
    private final JButton saveButton = new JButton("저장");
    private final JButton cancelButton = new JButton("취소");
    private final JLabel titleLabel = new JLabel("글 작성");
    private String boardKey;
    private Post editingPost; // null이면 새 글 작성

    public PostEditorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.attachmentPicker = new AttachmentPicker(mainFrame, true);
        saveButton.addActionListener(e -> save());
        // 취소는 저장하지 않고 목록으로만 돌아간다 — 새로고침할 필요가 없다.
        cancelButton.addActionListener(e -> mainFrame.switchTo("postList"));
        initLayout();
    }

    /** titleField/contentArea/attachmentPicker/saveButton을 배치하는 부분 — 디자인은 자유.
     *  (attachmentPicker는 그 자체가 JPanel이라 add(attachmentPicker) 한 줄이면 된다.) */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        add(titleLabel, BorderLayout.NORTH);

        // 본문(JTextArea)만 남는 공간을 전부 쓰고, 제목 한 줄과 첨부/버튼은 위아래로 붙인다.
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
        f.anchor = GridBagConstraints.LINE_START;
        f.gridx = 0;
        f.gridy = 0;
        f.gridwidth = 2;
        foot.add(attachmentPicker, f);

        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(cancelButton);
        f.gridy = 1;
        f.anchor = GridBagConstraints.CENTER;
        f.insets = new Insets(10, 4, 0, 4);
        foot.add(buttons, f);

        body.add(head, BorderLayout.NORTH);
        body.add(foot, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    /** mainFrame.switchTo("postEditor") 전에 반드시 먼저 호출한다. existingPost가 null이면 새 글 작성. */
    public void open(String boardKey, Post existingPost) {
        this.boardKey = boardKey;
        this.editingPost = existingPost;
        attachmentPicker.reset(
                existingPost == null ? null : existingPost.getFilePath(),
                existingPost == null ? null : existingPost.getImagePath());
        if (existingPost == null) {
            // 새 글: 이전에 열어둔 글의 값이 남아있지 않도록 전부 비운다.
            titleLabel.setText("글 작성");
            titleField.setText("");
            contentArea.setText("");
        } else {
            titleLabel.setText("글 수정");
            titleField.setText(existingPost.getTitle());
            contentArea.setText(existingPost.getContent());
        }
    }

    private void save() {
        String authorId = mainFrame.getCurrentUser().getId();
        boolean isNew = editingPost == null;
        // editingPost는 PostListPanel/PostDetailPanel이 아직 화면에 들고 있는 바로 그 객체라,
        // 서버 확인 전에 여기서 직접 고치면 POST_UPDATE가 실패해도 목록/상세에는 이미 새 내용이
        // (덮어쓴 것처럼) 보인다. 그래서 수정 시에도 새 Post 객체를 만들어 보낸다.
        Post post = isNew
                ? new Post(generateId(), titleField.getText(), authorId, contentArea.getText(),
                        attachmentPicker.getFilePath(), attachmentPicker.getImagePath(), LocalDateTime.now())
                : new Post(editingPost.getId(), titleField.getText(), editingPost.getAuthorId(), contentArea.getText(),
                        attachmentPicker.getFilePath(), attachmentPicker.getImagePath(), editingPost.getCreatedAt());
        RequestType type = isNew ? RequestType.POST_CREATE : RequestType.POST_UPDATE;
        Packet request = Packet.request(type, new PostCreateOrUpdateRequest(boardKey, post));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // 저장 결과를 바로 확인할 수 있게 목록으로 보낸다. 목록은 자기가 들고 있는 사본을
            // 그리므로 refresh()로 다시 받아오지 않으면 방금 쓴 글이 보이지 않는다.
            ((PostListPanel) mainFrame.getScreen("postList")).refresh();
            mainFrame.switchTo("postList");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 게시글 id 채번 규칙. 여러 클라이언트가 각자 만들어도 겹치지 않아야 해서 UUID를 쓴다
     * (서버는 중복 id를 거부하므로 겹치면 저장 자체가 실패한다).
     * NoticePostEditorPanel/GroupBuyPostEditorPanel/ComplaintPanel도 같은 규칙이다.
     */
    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
