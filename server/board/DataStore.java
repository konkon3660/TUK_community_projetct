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
 * 메서드를 그대로 사용한다. 새 학과가 생기면 registerBoards()에 한 줄만 추가하면 된다.
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

    private void registerBoards() {
        boards.put(BoardKey.FREE, new FreeBoard());
        boards.put(BoardKey.GROUP_BUY, new GroupBuyBoard());
        boards.put(BoardKey.DORM, new DormBoard());
        boards.put(BoardKey.NOTICE, new NoticeBoard());
        boards.put(BoardKey.COMPLAINT, new ComplaintBoard());

        // 학과별 게시판: boardKey == 학과명. 새 학과가 생기면 여기에 한 줄 추가.
        boards.put("AI소프트웨어학과",
                new DepartmentBoard("AI소프트웨어학과", "server/data/boards/class_boards/AI_software_board.dat"));
        boards.put("컴퓨터공학과",
                new DepartmentBoard("컴퓨터공학과", "server/data/boards/class_boards/computer_science.dat"));
        boards.put("물리학과",
                new DepartmentBoard("물리학과", "server/data/boards/class_boards/physics.dat"));
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
