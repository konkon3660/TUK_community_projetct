package client.CT;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import model.protocol.FileTransfer;
import model.protocol.Packet;
import model.protocol.RequestType;
import model.protocol.ResponseStatus;

/**
 * 첨부 업로드/다운로드 왕복을 한 곳에 모아둔 헬퍼. 각 GUI 패널이 직접 Packet을 조립하지 않도록 한다.
 *
 * <p>실패는 전부 예외로 던진다 — 호출하는 쪽(Swing 패널)에서 잡아 JOptionPane으로 보여주면 된다.
 */
public final class FileTransferClient {

    /** 로컬 파일을 서버로 올리고, Post.filePath/imagePath에 넣을 서버 저장 경로를 돌려준다. */
    public static String upload(ServerConnection connection, File file) {
        byte[] data;
        try {
            data = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException("파일을 읽을 수 없습니다: " + file.getName(), e);
        }
        // 서버도 같은 검사를 하지만, 5MB를 보내놓고 거절당하는 것보다 미리 막는 편이 빠르다.
        if (data.length > FileTransfer.MAX_BYTES) {
            throw new IllegalArgumentException("첨부는 최대 5MB까지 가능합니다: " + file.getName());
        }
        if (data.length == 0) {
            throw new IllegalArgumentException("빈 파일은 첨부할 수 없습니다: " + file.getName());
        }
        Packet response = connection.sendRequest(
                Packet.request(RequestType.FILE_UPLOAD, new FileTransfer(file.getName(), data)));
        requireOk(response);
        return (String) response.getPayload();
    }

    /** 게시글에 붙어 있는 서버 경로의 첨부를 받아온다 (이미지 표시 / 다른 이름으로 저장용). */
    public static FileTransfer download(ServerConnection connection, String storedPath) {
        Packet response = connection.sendRequest(Packet.request(RequestType.FILE_DOWNLOAD, storedPath));
        requireOk(response);
        return (FileTransfer) response.getPayload();
    }

    /** 받아온 첨부를 로컬에 저장 */
    public static void saveTo(FileTransfer file, Path target) {
        try {
            Files.write(target, file.getData());
        } catch (IOException e) {
            throw new UncheckedIOException("저장 실패: " + target, e);
        }
    }

    private static void requireOk(Packet response) {
        if (response.getStatus() != ResponseStatus.OK) {
            throw new IllegalStateException(response.getErrorMessage());
        }
    }

    private FileTransferClient() {
    }
}
