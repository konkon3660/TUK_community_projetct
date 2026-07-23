package client.GUI;

import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import client.CT.FileTransferClient;

/**
 * 게시글 에디터들이 공통으로 쓰는 "파일 첨부 / 이미지 첨부" 위젯.
 * 파일을 고르는 즉시 서버로 올려두고(FILE_UPLOAD), 게시글에 넣을 <b>서버 저장 경로만</b> 들고 있는다.
 * 에디터는 저장할 때 getFilePath()/getImagePath()를 Post 생성자나 setter에 그대로 넘기면 된다.
 *
 * <p>저장 버튼을 누르지 않고 화면을 벗어나면 올라간 파일이 게시글 없이 서버에 남지만,
 * 게시글이 그 경로를 참조하지 않으므로 데이터가 어긋나지는 않는다.
 */
public class AttachmentPicker extends JPanel {
    private final MainFrame mainFrame;
    private final JButton fileButton = new JButton("파일 첨부");
    private final JButton imageButton = new JButton("이미지 첨부");
    private final JButton clearButton = new JButton("첨부 지우기");
    private final JLabel statusLabel = new JLabel();
    private String filePath;  // 서버 저장 경로. 없으면 null
    private String imagePath; // 서버 저장 경로. 없으면 null

    /** allowFile이 false면 이미지만 받는다 (민원은 요구사항상 이미지 첨부만 있다). */
    public AttachmentPicker(MainFrame mainFrame, boolean allowFile) {
        this.mainFrame = mainFrame;
        fileButton.setVisible(allowFile);
        fileButton.addActionListener(e -> choose(false));
        imageButton.addActionListener(e -> choose(true));
        clearButton.addActionListener(e -> reset(null, null));

        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        add(fileButton);
        add(imageButton);
        add(clearButton);
        add(statusLabel);
        reset(null, null);
    }

    /** 에디터의 open(...)에서 호출. 새 글이면 (null, null), 수정이면 기존 게시글의 두 경로를 넘긴다. */
    public void reset(String filePath, String imagePath) {
        this.filePath = filePath;
        this.imagePath = imagePath;
        updateStatus();
    }

    public String getFilePath() {
        return filePath;
    }

    public String getImagePath() {
        return imagePath;
    }

    private void choose(boolean asImage) {
        JFileChooser chooser = new JFileChooser();
        if (asImage) {
            chooser.setFileFilter(new FileNameExtensionFilter("이미지 (png, jpg, gif)",
                    "png", "jpg", "jpeg", "gif"));
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selected = chooser.getSelectedFile();
        try {
            String storedPath = FileTransferClient.upload(mainFrame.getConnection(), selected);
            if (asImage) {
                imagePath = storedPath;
            } else {
                filePath = storedPath;
            }
            updateStatus();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "첨부 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus() {
        boolean hasAny = filePath != null || imagePath != null;
        clearButton.setEnabled(hasAny);
        if (!hasAny) {
            statusLabel.setText("첨부 없음");
            return;
        }
        StringBuilder text = new StringBuilder();
        if (filePath != null) {
            text.append("파일: ").append(displayName(filePath));
        }
        if (imagePath != null) {
            if (text.length() > 0) {
                text.append("  /  ");
            }
            text.append("이미지: ").append(displayName(imagePath));
        }
        statusLabel.setText(text.toString());
    }

    /** 서버 경로는 "server/data/files/UUID_원본이름" 형식 — 사용자에게는 원본 이름만 보여준다. */
    private static String displayName(String storedPath) {
        String name = storedPath.substring(storedPath.lastIndexOf('/') + 1);
        int separator = name.indexOf('_');
        return separator >= 0 && separator < name.length() - 1 ? name.substring(separator + 1) : name;
    }
}
