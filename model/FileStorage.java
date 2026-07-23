package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * .dat 파일 읽기/쓰기를 담당하는 공용 유틸. 각 클래스가 제각각 BufferedReader/Scanner 등으로
 * 파일을 다루지 않도록, 파일 입출력은 반드시 이 클래스를 통해서만 한다.
 */
public final class FileStorage {

    public static List<String> readLines(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        return Files.readAllLines(path);
    }

    public static void writeLines(Path path, List<String> lines) throws IOException {
        Files.write(path, lines);
    }

    /** 첨부파일/이미지처럼 텍스트가 아닌 파일용. 없는 파일이면 NoSuchFileException. */
    public static byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /** 첨부 저장 폴더(server/data/files)는 처음 업로드될 때 만들어지므로 상위 폴더를 함께 생성한다. */
    public static void writeBytes(Path path, byte[] data) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, data);
    }

    private FileStorage() {
    }
}
