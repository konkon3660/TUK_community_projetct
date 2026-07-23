package client.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    private final JButton cancelButton = new JButton("취소");
    private final JLabel titleLabel = new JLabel("공동구매 글 작성");
    private GroupBuyPost editingPost; // null이면 새 글 작성

    public GroupBuyPostEditorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.attachmentPicker = new AttachmentPicker(mainFrame, true);
        saveButton.addActionListener(e -> save());
        // 취소는 저장하지 않고 목록으로만 돌아간다 — 새로고침할 필요가 없다.
        cancelButton.addActionListener(e -> mainFrame.switchTo("postList"));
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유.
     *  (attachmentPicker는 그 자체가 JPanel이라 add(attachmentPicker) 한 줄이면 된다.) */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        add(titleLabel, BorderLayout.NORTH);

        // 본문(JTextArea)만 남는 공간을 전부 쓰고, 한 줄짜리 입력들은 위아래로 붙인다.
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
        foot.add(new JLabel("최대 인원"), f);

        f.gridx = 1;
        f.weightx = 1;
        f.fill = GridBagConstraints.HORIZONTAL;
        foot.add(maxMembersField, f);

        // 글쓴이가 방장 겸 첫 참여자로 들어가므로 1이면 만들자마자 가득 찬다 (서버도 같은 규칙으로 거부).
        f.gridx = 0;
        f.gridy = 1;
        f.gridwidth = 2;
        f.weightx = 0;
        f.fill = GridBagConstraints.NONE;
        f.anchor = GridBagConstraints.LINE_START;
        f.insets = new Insets(0, 4, 8, 4);
        foot.add(hint("※ 무제한은 -1, 그 외에는 2 이상"), f);

        f.gridy = 2;
        f.gridwidth = 1;
        f.anchor = GridBagConstraints.LINE_END;
        f.insets = new Insets(4, 4, 4, 4);
        foot.add(new JLabel("해시태그"), f);

        f.gridx = 1;
        f.weightx = 1;
        f.fill = GridBagConstraints.HORIZONTAL;
        foot.add(hashtagsField, f);

        f.gridx = 0;
        f.gridy = 3;
        f.gridwidth = 2;
        f.weightx = 0;
        f.fill = GridBagConstraints.NONE;
        f.anchor = GridBagConstraints.LINE_START;
        f.insets = new Insets(0, 4, 8, 4);
        foot.add(hint("※ 쉼표로 구분해 입력 (수정할 때는 바꿀 수 없음)"), f);

        f.gridy = 4;
        f.insets = new Insets(4, 4, 4, 4);
        foot.add(attachmentPicker, f);

        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(cancelButton);
        f.gridy = 5;
        f.anchor = GridBagConstraints.CENTER;
        f.insets = new Insets(10, 4, 0, 4);
        foot.add(buttons, f);

        body.add(head, BorderLayout.NORTH);
        body.add(foot, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    /** 입력 형식을 알려주는 작은 회색 안내 라벨 (NoticePostEditorPanel과 같은 스타일). */
    private JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(11f));
        label.setForeground(Color.GRAY);
        return label;
    }

    /** mainFrame.switchTo("groupBuyPostEditor") 전에 반드시 먼저 호출한다. existingPost가 null이면 새 글 작성. */
    public void open(GroupBuyPost existingPost) {
        this.editingPost = existingPost;
        attachmentPicker.reset(
                existingPost == null ? null : existingPost.getFilePath(),
                existingPost == null ? null : existingPost.getImagePath());
        if (existingPost == null) {
            // 새 글: 이전에 열어둔 글의 값이 남아있지 않도록 전부 비운다.
            titleLabel.setText("공동구매 글 작성");
            titleField.setText("");
            contentArea.setText("");
            maxMembersField.setText("");
            hashtagsField.setText("");
        } else {
            titleLabel.setText("공동구매 글 수정");
            titleField.setText(existingPost.getTitle());
            contentArea.setText(existingPost.getContent());
            maxMembersField.setText(String.valueOf(existingPost.getMaxMembers()));
            hashtagsField.setText(String.join(",", existingPost.getHashtags()));
        }
        // 수정할 때는 해시태그를 바꿀 수 없어서 아예 비활성화한다:
        // GroupBuyPost.hashtags는 final이라 세터가 없고, 서버의 POST_UPDATE도
        // 제목/내용/첨부/최대인원만 반영한다. 태그를 바꾸려면 글을 지우고 다시 써야 한다.
        hashtagsField.setEnabled(existingPost == null);
    }

    private void save() {
        int maxMembers;
        try {
            maxMembers = Integer.parseInt(maxMembersField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "최대 인원은 숫자로 입력하세요 (무제한은 -1).",
                    "저장 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String authorId = mainFrame.getCurrentUser().getId();
        boolean isNew = editingPost == null;
        GroupBuyPost post;
        if (isNew) {
            List<String> hashtags = parseHashtags(hashtagsField.getText());
            // chatRoomId는 null로 두면 된다 — 서버가 POST_CREATE 안에서 채팅방을 만들어 채운 뒤
            // 그 게시글을 응답으로 돌려준다 (CHATROOM_CREATE를 따로 보내지 말 것).
            post = new GroupBuyPost(generateId(), titleField.getText(), authorId, contentArea.getText(),
                    attachmentPicker.getFilePath(), attachmentPicker.getImagePath(), LocalDateTime.now(),
                    maxMembers, null, hashtags);
        } else {
            post = editingPost;
            post.setTitle(titleField.getText());
            post.setContent(contentArea.getText());
            post.setMaxMembers(maxMembers);
            // 첨부는 이미 FILE_UPLOAD로 올라가 있고, 여기서는 그 경로만 게시글에 반영한다.
            post.setFilePath(attachmentPicker.getFilePath());
            post.setImagePath(attachmentPicker.getImagePath());
        }
        RequestType type = isNew ? RequestType.POST_CREATE : RequestType.POST_UPDATE;
        Packet request = Packet.request(type, new PostCreateOrUpdateRequest(BoardKey.GROUP_BUY, post));
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

    /** "음식, 기숙사" 처럼 입력해도 되도록 앞뒤 공백을 떼고 빈 항목은 버린다. */
    private List<String> parseHashtags(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 게시글 id 채번 규칙. 여러 클라이언트가 각자 만들어도 겹치지 않아야 해서 UUID를 쓴다
     * (서버는 중복 id를 거부하므로 겹치면 저장 자체가 실패한다).
     * NoticePostEditorPanel/PostEditorPanel/ComplaintPanel도 같은 규칙이다.
     */
    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
