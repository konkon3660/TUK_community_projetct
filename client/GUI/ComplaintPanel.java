package client.GUI;

import java.time.LocalDateTime;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import model.boards.ComplaintPost;
import model.protocol.BoardKey;
import model.protocol.Packet;
import model.protocol.PostCreateOrUpdateRequest;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 민원 작성 화면. "내 문의 내역"은 PostListPanel.open(BoardKey.COMPLAINT)를 재사용해서 보여준다. */
public class ComplaintPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField titleField = new JTextField();
    private final JTextField category1Field = new JTextField();
    private final JTextField category2Field = new JTextField();
    private final JTextArea contentArea = new JTextArea();
    private final JButton submitButton = new JButton("제출");
    private final JButton myComplaintsButton = new JButton("내 문의 내역");
    private final JButton backButton = new JButton("뒤로");

    public ComplaintPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        submitButton.addActionListener(e -> submit());
        myComplaintsButton.addActionListener(e -> openMyComplaints());
        backButton.addActionListener(e -> mainFrame.switchTo("home"));
        initLayout();
    }

    /** 위 필드들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void submit() {
        String authorId = mainFrame.getCurrentUser().getId();
        ComplaintPost post = new ComplaintPost(generateId(), titleField.getText(), authorId, contentArea.getText(),
                null, null, LocalDateTime.now(), category1Field.getText(), category2Field.getText());
        Packet request = Packet.request(RequestType.POST_CREATE, new PostCreateOrUpdateRequest(BoardKey.COMPLAINT, post));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // TODO: 구현 필요. 예: JOptionPane.showMessageDialog(this, "접수되었습니다.");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "제출 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** ((PostListPanel) mainFrame.getScreen("postList")).open(BoardKey.COMPLAINT) 후 switchTo("postList"). */
    private void openMyComplaints() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** 새 글의 id를 어떻게 채번할지 아직 팀 논의가 없었다(UUID vs 다른 규칙). */
    private String generateId() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
