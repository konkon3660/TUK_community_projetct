package client.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import model.DataFormat;
import model.FileStorage;
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
    private final JButton faqButton = new JButton("자주 묻는 문의");
    private final JButton backButton = new JButton("뒤로");

    /** 자주 묻는 문의 템플릿 파일 (정적 데이터, 서버 통신 없음). 형식: 제목|1차분류|2차분류|내용 */
    private static final Path FAQ_PATH = Path.of("client/complaint_data/faq_templates.dat");

    public ComplaintPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.attachmentPicker = new AttachmentPicker(mainFrame, false);
        submitButton.addActionListener(e -> submit());
        myComplaintsButton.addActionListener(e -> openMyComplaints());
        faqButton.addActionListener(e -> pickFaqTemplate());
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
        buttons.add(faqButton);
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

    /** 템플릿 한 줄: 제목|1차분류|2차분류|내용. 말단 값이라 각 필드에 DataFormat.encode/decode를 씌운다. */
    private static class FaqTemplate {
        final String title, category1, category2, content;

        FaqTemplate(String title, String category1, String category2, String content) {
            this.title = title;
            this.category1 = category1;
            this.category2 = category2;
            this.content = content;
        }

        @Override
        public String toString() {
            return title; // 선택 다이얼로그 목록에 제목만 보이도록
        }
    }

    /** 템플릿 목록에서 하나를 골라 입력칸에 채운다. 이어서 사용자가 내용을 수정한 뒤 제출하면 된다. */
    private void pickFaqTemplate() {
        List<FaqTemplate> templates = loadFaqTemplates();
        if (templates.isEmpty()) {
            JOptionPane.showMessageDialog(this, "등록된 문의 템플릿이 없습니다.", "자주 묻는 문의", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        FaqTemplate chosen = (FaqTemplate) JOptionPane.showInputDialog(this,
                "불러올 문의를 고르세요", "자주 묻는 문의", JOptionPane.PLAIN_MESSAGE,
                null, templates.toArray(), templates.get(0));
        if (chosen == null) {
            return; // 취소
        }
        titleField.setText(chosen.title);
        category1Field.setText(chosen.category1);
        category2Field.setText(chosen.category2);
        contentArea.setText(chosen.content);
    }

    private List<FaqTemplate> loadFaqTemplates() {
        List<FaqTemplate> result = new ArrayList<>();
        try {
            for (String line : FileStorage.readLines(FAQ_PATH)) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] f = line.split("\\|", -1);
                if (f.length < 4) {
                    continue; // 형식이 맞지 않는 줄은 건너뛴다
                }
                result.add(new FaqTemplate(DataFormat.decode(f[0]), DataFormat.decode(f[1]),
                        DataFormat.decode(f[2]), DataFormat.decode(f[3])));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "문의 템플릿을 읽지 못했습니다: " + e.getMessage(),
                    "자주 묻는 문의", JOptionPane.ERROR_MESSAGE);
        }
        return result;
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
