package client.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
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

import model.boards.ComplaintPost;
import model.protocol.BoardKey;
import model.protocol.Packet;
import model.protocol.PostCreateOrUpdateRequest;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 민원 작성 화면(학생 전용). "내 문의 내역"은 PostListPanel.open(BoardKey.COMPLAINT, "home")을 재사용해서 보여준다. */
public class ComplaintPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField titleField = new JTextField();
    private final JTextField category1Field = new JTextField();
    private final JTextField category2Field = new JTextField();
    private final JTextArea contentArea = new JTextArea();
    // 민원의 추가 항목은 "이미지 첨부"만이라 파일 첨부 버튼은 띄우지 않는다 (02_requirements.md §4).
    private final AttachmentPicker attachmentPicker;
    private final JButton submitButton = new JButton("제출");
    private final JButton myComplaintsButton = new JButton("내 문의 내역");
    private final JButton backButton = new JButton("뒤로");

    public ComplaintPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.attachmentPicker = new AttachmentPicker(mainFrame, false);
        submitButton.addActionListener(e -> submit());
        myComplaintsButton.addActionListener(e -> openMyComplaints());
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        JLabel title = new JLabel("민원 접수");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        // 한 줄짜리 입력들은 위에 모으고, 내용(JTextArea)이 남는 공간을 전부 쓰게 한다.
        JPanel head = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_END;
        head.add(new JLabel("제목"), c);
        c.gridy = 1;
        head.add(new JLabel("문의 분류"), c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 3;
        c.weightx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        head.add(titleField, c);

        // 1차/2차 카테고리는 한 줄에 나란히
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.5;
        head.add(category1Field, c);
        c.gridx = 2;
        head.add(category2Field, c);

        JLabel hint = new JLabel("※ 1차 · 2차 분류 순서로 입력 (예: 시설 / 냉난방)");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(Color.GRAY);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 4, 8, 4);
        head.add(hint, c);

        JPanel body = new JPanel(new BorderLayout(0, 6));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        body.add(head, BorderLayout.NORTH);
        body.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout(0, 6));
        foot.add(attachmentPicker, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.add(submitButton);
        buttons.add(myComplaintsButton);
        buttons.add(backButton);
        foot.add(buttons, BorderLayout.SOUTH);
        body.add(foot, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);
    }

    private void submit() {
        String authorId = mainFrame.getCurrentUser().getId();
        ComplaintPost post = new ComplaintPost(generateId(), titleField.getText(), authorId, contentArea.getText(),
                null, attachmentPicker.getImagePath(),
                LocalDateTime.now(), category1Field.getText(), category2Field.getText());
        Packet request = Packet.request(RequestType.POST_CREATE, new PostCreateOrUpdateRequest(BoardKey.COMPLAINT, post));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            JOptionPane.showMessageDialog(this,
                    "접수되었습니다. 답변이 등록되면 '내 문의 내역'에서 확인할 수 있습니다.",
                    "민원 접수 완료", JOptionPane.INFORMATION_MESSAGE);
            clearFields(); // 다음 민원을 쓸 때 앞 내용이 남아있지 않도록
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "제출 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        titleField.setText("");
        category1Field.setText("");
        category2Field.setText("");
        contentArea.setText("");
        attachmentPicker.reset(null, null);
    }

    /** ((PostListPanel) mainFrame.getScreen("postList")).open(BoardKey.COMPLAINT, "home") 후 switchTo("postList"). */
    private void openMyComplaints() {
        // 서버가 일반 유저에게는 본인이 넣은 민원만 돌려주므로 목록 화면을 그대로 재사용한다.
        ((PostListPanel) mainFrame.getScreen("postList")).open(BoardKey.COMPLAINT, "home");
        mainFrame.switchTo("postList");
    }

    /** 게시글 id 채번 규칙 — NoticePostEditorPanel과 동일하게 UUID를 쓴다(서버가 중복 id는 거부). */
    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
