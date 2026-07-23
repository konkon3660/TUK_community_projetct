package server.board;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import model.AcademicStructure;
import model.ChatRoom;
import model.FileStorage;
import model.User;
import model.boards.Board;
import model.boards.ComplaintBoard;
import model.boards.DepartmentBoard;
import model.boards.DormBoard;
import model.boards.FreeBoard;
import model.boards.GroupBuyBoard;
import model.boards.NoticeBoard;
import model.protocol.BoardKey;

/**
 * 서버 시작 시 한 번 생성되어 게시판/유저/채팅방을 메모리에 올려두는 레지스트리.
 * 파일 입출력 배관만 담당하고 권한 체크 등 업무 로직은 넣지 않는다 — model/의 기존
 * 메서드를 그대로 사용한다. 학과별 게시판은 {@link AcademicStructure}를 순회해서 자동으로
 * 등록되므로, 새 학과가 생겨도 이 클래스는 손댈 필요가 없다 — 조직도에만 추가하면 된다.
 */
public class DataStore {
    private static final Path USERS_PATH = Path.of("server/data/users.dat");
    private static final Path CHATROOMS_DIR = Path.of("server/data/chatrooms");

    private final Map<String, Board> boards = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, ChatRoom> chatRooms = new HashMap<>();

    public DataStore() {
        registerBoards();
        loadAll();
    }

    private static final String CLASS_BOARDS_DIR = "server/data/boards/class_boards/";

    private void registerBoards() {
        boards.put(BoardKey.FREE, new FreeBoard());
        boards.put(BoardKey.GROUP_BUY, new GroupBuyBoard());
        boards.put(BoardKey.DORM, new DormBoard());
        boards.put(BoardKey.NOTICE, new NoticeBoard());
        boards.put(BoardKey.COMPLAINT, new ComplaintBoard());

        // 학과별 게시판: boardKey == 학과명. AcademicStructure의 리프(단과대→학부→학과) 전체를
        // 순회해서 한 번에 등록한다 — 학과가 40개가 넘어 손으로 나열하면 실수하기 쉽고, 트리에
        // 새 학과가 추가돼도 여기 코드를 바꿀 필요 없이 게시판이 자동으로 생긴다.
        // 파일이 없는 학과(아직 아무도 글을 안 쓴 곳)는 AbstractBoard.load()가 빈 목록으로
        // 시작하고, 첫 글이 저장될 때 save()가 파일을 새로 만든다.
        for (AcademicStructure.College college : AcademicStructure.COLLEGES) {
            for (AcademicStructure.Division division : college.getDivisions()) {
                for (AcademicStructure.Department department : division.getDepartments()) {
                    String name = department.getName();
                    boards.put(name, new DepartmentBoard(name, CLASS_BOARDS_DIR + name + ".dat"));
                }
            }
        }
    }

    private void loadAll() {
        for (Board board : boards.values()) {
            board.load();
        }
        loadUsers();
        loadChatRooms();
    }

    public Board getBoard(String boardKey) {
        Board board = boards.get(boardKey);
        if (board == null) {
            throw new NoSuchElementException("존재하지 않는 게시판: " + boardKey);
        }
        return board;
    }

    public User getUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new NoSuchElementException("존재하지 않는 유저: " + userId);
        }
        return user;
    }

    /** 회원가입 시 학번 중복 확인용 (getUser는 없으면 예외를 던지므로 검사에 쓰지 않는다). */
    public boolean hasUser(String userId) {
        return users.containsKey(userId);
    }

    public void addUser(User user) {
        users.put(user.getId(), user);
        saveUsers();
    }

    public ChatRoom getChatRoom(String roomId) {
        ChatRoom room = chatRooms.get(roomId);
        if (room == null) {
            throw new NoSuchElementException("존재하지 않는 채팅방: " + roomId);
        }
        return room;
    }

    public void addChatRoom(ChatRoom room) {
        chatRooms.put(room.getRoomId(), room);
        saveChatRoom(room);
    }

    public List<ChatRoom> getAllChatRooms() {
        return new ArrayList<>(chatRooms.values());
    }

    /** 방장 삭제 요청 처리용. 메모리와 .dat 파일 양쪽에서 지운다. */
    public void removeChatRoom(String roomId) {
        getChatRoom(roomId); // 없으면 예외
        chatRooms.remove(roomId);
        try {
            Files.deleteIfExists(chatRoomPath(roomId));
        } catch (IOException e) {
            throw new UncheckedIOException("채팅방 삭제 실패: " + roomId, e);
        }
    }

    public void saveChatRoom(ChatRoom room) {
        try {
            FileStorage.writeLines(chatRoomPath(room.getRoomId()),
                    Collections.singletonList(room.toDataString()));
        } catch (IOException e) {
            throw new UncheckedIOException("채팅방 저장 실패: " + room.getRoomId(), e);
        }
    }

    private void loadUsers() {
        try {
            for (String line : FileStorage.readLines(USERS_PATH)) {
                if (!line.isEmpty()) {
                    User user = User.fromDataString(line);
                    users.put(user.getId(), user);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("유저 목록 로드 실패", e);
        }
    }

    public void saveUsers() {
        try {
            FileStorage.writeLines(USERS_PATH,
                    users.values().stream().map(User::toDataString).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new UncheckedIOException("유저 목록 저장 실패", e);
        }
    }

    private void loadChatRooms() {
        if (!Files.isDirectory(CHATROOMS_DIR)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(CHATROOMS_DIR, "*.dat")) {
            for (Path file : files) {
                for (String line : FileStorage.readLines(file)) {
                    if (!line.isEmpty()) {
                        ChatRoom room = ChatRoom.fromDataString(line);
                        chatRooms.put(room.getRoomId(), room);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("채팅방 목록 로드 실패", e);
        }
    }

    private Path chatRoomPath(String roomId) {
        return CHATROOMS_DIR.resolve(roomId + ".dat");
    }
}
