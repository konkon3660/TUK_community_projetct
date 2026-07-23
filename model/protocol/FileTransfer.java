package model.protocol;

import java.io.Serializable;

/**
 * 첨부파일/이미지 한 개의 "원본 파일명 + 내용" 한 벌.
 * FILE_UPLOAD의 요청 payload와 FILE_DOWNLOAD의 응답 payload에 같은 모양으로 쓴다.
 *
 * <p>Post.filePath/imagePath에는 이 객체가 아니라 FILE_UPLOAD가 돌려준 <b>서버 저장 경로(String)</b>가
 * 들어간다 — 게시글 목록(POST_LIST)을 받을 때마다 첨부 내용까지 딸려오지 않게 하기 위해서다.
 */
public class FileTransfer implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 첨부 1개의 최대 크기 (02_requirements.md §2.1 "파일 경로 (최대 5MB)") */
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    private final String fileName; // 확장자 포함 원본 파일명. 경로 성분은 담지 않는다.
    private final byte[] data;

    public FileTransfer(String fileName, byte[] data) {
        this.fileName = fileName;
        // 불변으로 유지하기 위한 방어적 복사 (최대 5MB로 제한되어 있어 복사 비용이 문제되지 않는다)
        this.data = data.clone();
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data.clone();
    }

    public int size() {
        return data.length;
    }
}
