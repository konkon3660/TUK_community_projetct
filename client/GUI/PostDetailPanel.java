package client.GUI;

import java.io.File;
import java.time.LocalDateTime;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import client.CT.FileTransferClient;
import model.boards.Comment;
import model.boards.Post;
import model.protocol.CommentAddRequest;
import model.protocol.CommentDeleteRequest;
import model.protocol.FileTransfer;
import model.protocol.Packet;
import model.protocol.PostDeleteRequest;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/** 게시글 상세 + 댓글. 모든 boardKey/Post 하위타입에서 공용으로 쓴다. */
public class PostDetailPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JButton editButton = new JButton("수정");
    private final JButton deleteButton = new JButton("삭제");
    private final JTextField commentField = new JTextField();
    private final JButton addCommentButton = new JButton("댓글 등록");
    private final JButton backButton = new JButton("뒤로");
    private String boardKey;
    private Post post;

    public PostDetailPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        editButton.addActionListener(e -> openEditor());
        deleteButton.addActionListener(e -> deletePost());
        addCommentButton.addActionListener(e -> addComment());
        backButton.addActionListener(e -> mainFrame.switchTo("postList"));
        initLayout();
    }

    /** 제목/본문/작성자/첨부/댓글 목록 + 위 버튼들을 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    /** mainFrame.switchTo("postDetail") 전에 반드시 먼저 호출한다. */
    public void open(String boardKey, Post post) {
        this.boardKey = boardKey;
        this.post = post;
        renderPost();
    }

    /** post의 필드/댓글을 화면에 채우고, post.canEdit(mainFrame.getCurrentUser())/canDelete(...)에 따라
     *  editButton/deleteButton 노출 여부를 정하는 부분 — 렌더링은 자유.
     *  첨부는 post.getFilePath()/getImagePath()가 null이 아닐 때만 버튼을 띄우고
     *  saveAttachment(경로) / loadImage(경로)를 연결하면 된다. */
    private void renderPost() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
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

    /** 첨부 이미지를 화면에 바로 붙일 수 있는 형태로 받아온다. 실패하면 null(= 이미지 영역을 감춘다). */
    private ImageIcon loadImage(String storedPath) {
        try {
            return new ImageIcon(FileTransferClient.download(mainFrame.getConnection(), storedPath).getData());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void openEditor() {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private void deletePost() {
        Packet request = Packet.request(RequestType.POST_DELETE, new PostDeleteRequest(boardKey, post.getId()));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // TODO: 구현 필요. 예: mainFrame.switchTo("postList");
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "삭제 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addComment() {
        String authorId = mainFrame.getCurrentUser().getId();
        Comment comment = new Comment(authorId, commentField.getText(), LocalDateTime.now());
        Packet request = Packet.request(RequestType.COMMENT_ADD,
                new CommentAddRequest(boardKey, post.getId(), comment));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // TODO: 구현 필요. 예: post.addComment(comment); renderPost();
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "댓글 등록 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** commentIndex는 post.getComments() 리스트 내 위치(0부터). */
    private void deleteComment(int commentIndex) {
        Packet request = Packet.request(RequestType.COMMENT_DELETE,
                new CommentDeleteRequest(boardKey, post.getId(), commentIndex));
        Packet response = mainFrame.getConnection().sendRequest(request);
        if (response.getStatus() == ResponseStatus.OK) {
            // TODO: 구현 필요. 예: post.getComments().remove(commentIndex); renderPost();
        } else {
            JOptionPane.showMessageDialog(this, response.getErrorMessage(), "댓글 삭제 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
