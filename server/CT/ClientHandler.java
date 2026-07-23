package server.CT;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import model.Chat;
import model.ChatRoom;
import model.FileStorage;
import model.User;
import model.boards.Board;
import model.boards.Comment;
import model.boards.ComplaintBoard;
import model.boards.ComplaintPost;
import model.boards.GroupBuyBoard;
import model.boards.GroupBuyPost;
import model.boards.NoticeBoard;
import model.boards.NoticePost;
import model.boards.Post;
import model.protocol.ChatPushPayload;
import model.protocol.ChatRoomJoinDecision;
import model.protocol.ChatRoomJoinRequest;
import model.protocol.ChatRoomNicknameRequest;
import model.protocol.ChatSendRequest;
import model.protocol.CommentAddRequest;
import model.protocol.CommentDeleteRequest;
import model.protocol.FileTransfer;
import model.protocol.LoginRequest;
import model.protocol.Packet;
import model.protocol.PostCreateOrUpdateRequest;
import model.protocol.PostDeleteRequest;
import model.protocol.RequestType;
import server.board.DataStore;

/**
 * 클라이언트 소켓 하나당 스레드 하나. 패킷을 읽어 type으로 분기하고, 실제 업무 로직은
 * handleXxx 메서드에만 채운다 (연결/직렬화/동기화 배관 코드는 이미 완성되어 있음).
 */
public class ClientHandler implements Runnable {
    // CUD에 해당하는 요청은 DATA_LOCK으로 감싸서 처리한다. 순수 조회(POST_LIST)는 감싸지 않는다.
    // FILE_UPLOAD는 파일을 쓰지만 여기 넣지 않는다 — 매번 새 이름(UUID)으로만 저장해서 서로 겹치지
    // 않고 메모리 상의 게시판/유저도 건드리지 않는데, 최대 5MB 쓰기를 락 안에서 하면 그동안 다른
    // 모든 CUD가 멈추기 때문이다.
    private static final Set<RequestType> SYNCHRONIZED_TYPES = EnumSet.of(
            RequestType.REGISTER, RequestType.USER_UPDATE,
            RequestType.POST_CREATE, RequestType.POST_UPDATE, RequestType.POST_DELETE,
            RequestType.COMMENT_ADD, RequestType.COMMENT_DELETE,
            RequestType.CHATROOM_CREATE, RequestType.CHATROOM_JOIN_REQUEST,
            RequestType.CHATROOM_JOIN_APPROVE, RequestType.CHATROOM_JOIN_REJECT,
            RequestType.CHAT_SEND, RequestType.CHATROOM_SET_NICKNAME, RequestType.CHATROOM_DELETE);

    /** 업로드된 첨부파일/이미지가 쌓이는 곳. 게시판 .dat들과 같은 server/data 아래에 둔다. */
    private static final Path UPLOAD_DIR = Path.of("server/data/files");

    private static final Object DATA_LOCK = new Object();

    private final Socket socket;
    private final DataStore dataStore;
    private final SessionRegistry sessionRegistry;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile User currentUser; // 로그인 성공 시 세팅 (푸시 스레드가 읽으므로 volatile)

    public ClientHandler(Socket socket, DataStore dataStore, SessionRegistry sessionRegistry) {
        this.socket = socket;
        this.dataStore = dataStore;
        this.sessionRegistry = sessionRegistry;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void run() {
        try {
            // ObjectOutputStream을 먼저 만들고 flush 해야 상대방 ObjectInputStream 생성이 블로킹되지 않는다.
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (!socket.isClosed()) {
                Packet request = (Packet) in.readObject();
                if (request == null || request.getType() == RequestType.DISCONNECT) {
                    break;
                }
                handleRequest(request);
            }
        } catch (IOException | ClassNotFoundException e) {
            // 연결이 끊긴 경우 정상 종료로 취급
        } finally {
            clearSession();
            closeQuietly();
        }
    }

    private void handleRequest(Packet request) {
        try {
            Packet response;
            if (SYNCHRONIZED_TYPES.contains(request.getType())) {
                synchronized (DATA_LOCK) {
                    response = dispatch(request);
                }
            } else {
                response = dispatch(request);
            }
            sendPacket(response);
        } catch (Exception e) {
            sendPacket(Packet.error(request, e.getMessage()));
        }
    }

    private Packet dispatch(Packet request) {
        switch (request.getType()) {
            case LOGIN:
                return handleLogin(request);
            case REGISTER:
                return handleRegister(request);
            case LOGOUT:
                return handleLogout(request);
            case USER_UPDATE:
                return handleUserUpdate(request);
            case USER_LOOKUP:
                return handleUserLookup(request);
            case POST_LIST:
                return handlePostList(request);
            case POST_CREATE:
                return handlePostCreate(request);
            case POST_UPDATE:
                return handlePostUpdate(request);
            case POST_DELETE:
                return handlePostDelete(request);
            case COMMENT_ADD:
                return handleCommentAdd(request);
            case COMMENT_DELETE:
                return handleCommentDelete(request);
            case FILE_UPLOAD:
                return handleFileUpload(request);
            case FILE_DOWNLOAD:
                return handleFileDownload(request);
            case CHATROOM_CREATE:
                return handleChatRoomCreate(request);
            case CHATROOM_JOIN_REQUEST:
                return handleChatRoomJoinRequest(request);
            case CHATROOM_JOIN_APPROVE:
                return handleChatRoomJoinApprove(request);
            case CHATROOM_JOIN_REJECT:
                return handleChatRoomJoinReject(request);
            case CHAT_SEND:
                return handleChatSend(request);
            case CHATROOM_SET_NICKNAME:
                return handleChatRoomSetNickname(request);
            case CHATROOM_LIST:
                return handleChatRoomList(request);
            case CHATROOM_DELETE:
                return handleChatRoomDelete(request);
            default:
                throw new IllegalArgumentException("클라이언트가 보낼 수 없는 요청 타입: " + request.getType());
        }
    }

    /** 패킷 전송 공용 헬퍼. 다른 스레드(다른 ClientHandler 등)에서 이 클라이언트로 푸시를 보낼 때도 사용한다. */
    public void sendPacket(Packet packet) {
        try {
            synchronized (this) {
                out.writeObject(packet);
                // 직렬화 캐시(handle) 때문에 같은 객체를 재전송해도 예전 상태가 나가는 것을 방지
                out.reset();
                out.flush();
            }
        } catch (IOException e) {
            closeQuietly();
        }
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    // ── 아래는 실제 업무 로직 ────────────────────────────────────────────────
    // 검증에 실패하면 예외를 그냥 던지면 된다 — handleRequest가 잡아서
    // Packet.error(request, 메시지)로 바꿔 보낸다. 메시지는 사용자에게 그대로 보이므로 한국어로 쓴다.

    private Packet handleLogin(Packet request) {
        LoginRequest loginRequest = (LoginRequest) request.getPayload();
        User user;
        try {
            user = dataStore.getUser(loginRequest.getId());
        } catch (NoSuchElementException e) {
            // 존재하지 않는 학번인지 비밀번호가 틀린 건지 구분해서 알려주지 않는다.
            throw new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다");
        }
        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다");
        }
        currentUser = user;
        sessionRegistry.register(user.getId(), this);
        return Packet.success(request, user);
    }

    private Packet handleRegister(Packet request) {
        User newUser = (User) request.getPayload();
        if (isBlank(newUser.getId()) || isBlank(newUser.getPassword())) {
            throw new IllegalArgumentException("학번과 비밀번호는 필수입니다");
        }
        if (isBlank(newUser.getDepartment())) {
            throw new IllegalArgumentException("학과는 필수입니다");
        }
        if (dataStore.hasUser(newUser.getId())) {
            throw new IllegalStateException("이미 존재하는 학번입니다: " + newUser.getId());
        }
        // 관리자 여부는 클라이언트가 보낸 값을 믿지 않는다 (가입은 항상 일반 유저).
        dataStore.addUser(new User(newUser.getId(), newUser.getDepartment(),
                newUser.isDormitory(), newUser.getPassword(), false));
        return Packet.success(request, null);
    }

    private Packet handleLogout(Packet request) {
        clearSession();
        return Packet.success(request, null);
    }

    private Packet handleUserUpdate(Packet request) {
        requireAdmin();
        User updated = (User) request.getPayload();
        User stored = dataStore.getUser(updated.getId()); // 없으면 예외
        // id와 admin은 final이라 바꿀 수 없다 — 나머지 세 항목만 반영한다.
        stored.setDepartment(updated.getDepartment());
        stored.setDormitory(updated.isDormitory());
        stored.setPassword(updated.getPassword());
        dataStore.saveUsers();
        return Packet.success(request, null);
    }

    /** 회원정보 수정 화면에서 학번으로 회원 1명을 찾는 용도. User에는 비밀번호가 들어 있으므로 관리자 전용. */
    private Packet handleUserLookup(Packet request) {
        requireAdmin();
        String userId = (String) request.getPayload();
        return Packet.success(request, dataStore.getUser(userId)); // 없으면 NoSuchElementException
    }

    private Packet handlePostList(Packet request) {
        requireLogin();
        String boardKey = (String) request.getPayload();
        Board board = dataStore.getBoard(boardKey);
        List<Post> visible;
        if (board instanceof NoticeBoard) {
            // 공지는 전체 열람 가능하지만 대상 학과/기숙사에 맞는 것만 보여준다.
            visible = ((NoticeBoard) board).getVisiblePosts(currentUser);
        } else if (board instanceof ComplaintBoard && !currentUser.isAdmin()) {
            // 일반 유저는 "문의 내역" — 본인이 넣은 민원만.
            visible = ((ComplaintBoard) board).getPostsByAuthor(currentUser.getId());
        } else {
            requireAccess(board, boardKey);
            visible = board.getPosts();
        }
        // 서버가 들고 있는 리스트를 그대로 넘기면 다른 스레드가 수정하는 중에 직렬화될 수 있다.
        return Packet.success(request, new ArrayList<>(visible));
    }

    private Packet handlePostCreate(Packet request) {
        requireLogin();
        PostCreateOrUpdateRequest payload = (PostCreateOrUpdateRequest) request.getPayload();
        String boardKey = payload.getBoardKey();
        Board board = dataStore.getBoard(boardKey);
        Post post = payload.getPost();

        if (board instanceof NoticeBoard) {
            if (!((NoticeBoard) board).canWrite(currentUser)) {
                throw new IllegalStateException("공지는 관리자만 작성할 수 있습니다");
            }
        } else if (!(board instanceof ComplaintBoard)) {
            // 민원은 canAccess가 "전체 민원함 열람" 기준이라 등록에는 적용하지 않는다
            // (누구나 넣을 수 있고, 열람만 본인 것으로 제한된다).
            requireAccess(board, boardKey);
        }
        requirePostType(board, post);
        if (isBlank(post.getId())) {
            throw new IllegalArgumentException("게시글 id가 비어 있습니다");
        }
        if (!post.getAuthorId().equals(currentUser.getId())) {
            throw new IllegalStateException("다른 사람 명의로 글을 쓸 수 없습니다");
        }
        if (board.getPosts().stream().anyMatch(p -> p.getId().equals(post.getId()))) {
            throw new IllegalStateException("이미 존재하는 게시글 id입니다: " + post.getId());
        }

        // 공동구매 글에는 참여자 채팅방이 반드시 딸려야 한다. 클라이언트가 CHATROOM_CREATE와
        // POST_CREATE를 따로 보내면 그 사이에 끊겼을 때 주인 없는 채팅방이 남으므로, 이 요청
        // 하나(=DATA_LOCK 한 번) 안에서 방까지 만든다. 클라이언트가 채워 보낸 chatRoomId는
        // 신뢰하지 않고 항상 서버가 채번한 값으로 덮어쓴다.
        ChatRoom linkedRoom = null;
        if (post instanceof GroupBuyPost) {
            GroupBuyPost groupBuyPost = (GroupBuyPost) post;
            linkedRoom = createLinkedChatRoom(groupBuyPost);
            groupBuyPost.setChatRoomId(linkedRoom.getRoomId());
        }

        board.addPost(post);
        board.save();
        if (linkedRoom != null) {
            // 게시글 저장이 끝난 뒤에 등록한다 — board.save()가 실패하면 채팅방은 아직
            // dataStore에 들어가지 않은 상태라 고아 방이 남지 않는다.
            dataStore.addChatRoom(linkedRoom);
        }

        if (post instanceof NoticePost) {
            NoticePost notice = (NoticePost) post;
            sessionRegistry.sendToAll(Packet.push(RequestType.NOTICE_PUSH, notice), notice::isVisibleTo);
        }
        return Packet.success(request, post);
    }

    private Packet handlePostUpdate(Packet request) {
        requireLogin();
        PostCreateOrUpdateRequest payload = (PostCreateOrUpdateRequest) request.getPayload();
        Board board = dataStore.getBoard(payload.getBoardKey());
        Post incoming = payload.getPost();
        Post stored = findPost(board, incoming.getId());
        if (!stored.canEdit(currentUser)) {
            throw new IllegalStateException("수정 권한이 없습니다: " + currentUser.getId());
        }
        // 댓글 목록은 서버 것이 정본이므로 통째로 갈아끼우지 않고 수정 가능한 필드만 옮긴다.
        stored.setTitle(incoming.getTitle());
        stored.setContent(incoming.getContent());
        stored.setFilePath(incoming.getFilePath());
        stored.setImagePath(incoming.getImagePath());
        if (stored instanceof GroupBuyPost && incoming instanceof GroupBuyPost) {
            GroupBuyPost storedGroupBuy = (GroupBuyPost) stored;
            int maxMembers = requireGroupBuyMaxMembers(((GroupBuyPost) incoming).getMaxMembers());
            storedGroupBuy.setMaxMembers(maxMembers);
            // chatRoomId는 POST_CREATE에서 서버가 정한 뒤로 바뀌지 않는다 — 클라이언트가 보낸
            // 값으로 덮어쓰면 남의 방을 가리키게 만들거나 연결을 끊어버릴 수 있다.
            // 대신 정원은 "현재 인원수 = 채팅방 참여자 수" 규칙이 깨지지 않도록 방에도 반영한다.
            // 연동 도입 전에 저장된 .dat 줄에는 chatRoomId가 비어 있을 수 있다 — 그런 글은 건너뛴다.
            if (!isBlank(storedGroupBuy.getChatRoomId())) {
                ChatRoom linkedRoom = dataStore.getChatRoom(storedGroupBuy.getChatRoomId());
                linkedRoom.setMaxMembers(maxMembers);
                dataStore.saveChatRoom(linkedRoom);
            }
        }
        board.save();
        return Packet.success(request, null);
    }

    private Packet handlePostDelete(Packet request) {
        requireLogin();
        PostDeleteRequest payload = (PostDeleteRequest) request.getPayload();
        Board board = dataStore.getBoard(payload.getBoardKey());
        board.removePost(payload.getPostId(), currentUser); // 권한 검사는 AbstractBoard가 수행
        board.save();
        return Packet.success(request, null);
    }

    private Packet handleCommentAdd(Packet request) {
        requireLogin();
        CommentAddRequest payload = (CommentAddRequest) request.getPayload();
        Board board = dataStore.getBoard(payload.getBoardKey());
        Post post = findPost(board, payload.getPostId());
        Comment comment = payload.getComment();
        if (!comment.getAuthorId().equals(currentUser.getId())) {
            throw new IllegalStateException("다른 사람 명의로 댓글을 쓸 수 없습니다");
        }
        // 민원은 열람 자체가 관리자/작성자 본인으로 제한되므로 댓글도 같은 기준으로 막는다.
        if (board instanceof ComplaintBoard
                && !currentUser.isAdmin() && !post.getAuthorId().equals(currentUser.getId())) {
            throw new IllegalStateException("열람 권한이 없는 민원입니다: " + post.getId());
        }
        post.addComment(comment);
        // 관리자가 민원에 단 댓글 = 답변이므로 상태도 함께 바꾼다 (datastruct.md 3.3).
        if (post instanceof ComplaintPost && currentUser.isAdmin()) {
            ((ComplaintPost) post).markAnswered();
        }
        board.save();
        return Packet.success(request, null);
    }

    private Packet handleCommentDelete(Packet request) {
        requireLogin();
        CommentDeleteRequest payload = (CommentDeleteRequest) request.getPayload();
        Board board = dataStore.getBoard(payload.getBoardKey());
        Post post = findPost(board, payload.getPostId());
        int index = payload.getCommentIndex();
        if (index < 0 || index >= post.getComments().size()) {
            throw new NoSuchElementException("댓글을 찾을 수 없음: " + index);
        }
        post.removeComment(post.getComments().get(index), currentUser); // 권한 검사는 Post가 수행
        board.save();
        return Packet.success(request, null);
    }

    /**
     * 첨부를 server/data/files 아래에 "UUID_원본이름"으로 저장하고 그 경로를 돌려준다.
     * 클라이언트는 이 경로를 Post.filePath/imagePath에 넣어 POST_CREATE/POST_UPDATE를 보낸다.
     * 이름 앞에 UUID를 붙이는 이유는 서로 다른 사람이 같은 이름("사진.png")으로 올려도 덮어쓰지 않게 하기 위해서다.
     */
    private Packet handleFileUpload(Packet request) {
        requireLogin();
        FileTransfer upload = (FileTransfer) request.getPayload();
        if (upload.size() == 0) {
            throw new IllegalArgumentException("빈 파일은 첨부할 수 없습니다");
        }
        if (upload.size() > FileTransfer.MAX_BYTES) {
            throw new IllegalArgumentException("첨부는 최대 5MB까지 가능합니다");
        }
        String storedName = UUID.randomUUID() + "_" + sanitizeFileName(upload.getFileName());
        try {
            FileStorage.writeBytes(UPLOAD_DIR.resolve(storedName), upload.getData());
        } catch (IOException e) {
            throw new UncheckedIOException("첨부 저장 실패: " + upload.getFileName(), e);
        }
        // 이 값이 .dat 한 줄에 그대로 들어가므로 OS별 구분자(\)가 아니라 항상 '/'로 맞춘다.
        return Packet.success(request, UPLOAD_DIR.toString().replace('\\', '/') + "/" + storedName);
    }

    private Packet handleFileDownload(Packet request) {
        requireLogin();
        String storedPath = (String) request.getPayload();
        Path target = resolveUpload(storedPath);
        try {
            return Packet.success(request,
                    new FileTransfer(originalFileName(target.getFileName().toString()),
                            FileStorage.readBytes(target)));
        } catch (IOException e) {
            throw new UncheckedIOException("첨부 읽기 실패: " + storedPath, e);
        }
    }

    private Packet handleChatRoomCreate(Packet request) {
        requireLogin();
        ChatRoom template = (ChatRoom) request.getPayload();
        // roomId는 클라이언트가 보낸 값을 쓰지 않고 서버가 채번한다 (001, 002, ... 형식).
        ChatRoom room = new ChatRoom(nextRoomId(), currentUser.getId(), template.getMaxMembers());
        room.setName(template.getName());
        room.setAdmissionYearLimit(template.getAdmissionYearLimit());
        room.getDepartmentLimit().addAll(template.getDepartmentLimit());
        room.setDormOnlyLimit(template.isDormOnlyLimit());
        room.setInviteBypassesLimit(template.isInviteBypassesLimit());
        room.getMemberIds().add(currentUser.getId()); // 방장은 바로 참여자
        dataStore.addChatRoom(room);
        return Packet.success(request, room);
    }

    private Packet handleChatRoomJoinRequest(Packet request) {
        requireLogin();
        ChatRoomJoinRequest payload = (ChatRoomJoinRequest) request.getPayload();
        ChatRoom room = dataStore.getChatRoom(payload.getRoomId());
        room.requestJoin(currentUser, payload.getMessage());
        dataStore.saveChatRoom(room);
        return Packet.success(request, null);
    }

    private Packet handleChatRoomJoinApprove(Packet request) {
        requireLogin();
        ChatRoomJoinDecision payload = (ChatRoomJoinDecision) request.getPayload();
        ChatRoom room = requireOwnedRoom(payload.getRoomId());
        room.approveJoin(payload.getUserId());
        dataStore.saveChatRoom(room);
        return Packet.success(request, null);
    }

    private Packet handleChatRoomJoinReject(Packet request) {
        requireLogin();
        ChatRoomJoinDecision payload = (ChatRoomJoinDecision) request.getPayload();
        ChatRoom room = requireOwnedRoom(payload.getRoomId());
        room.rejectJoin(payload.getUserId());
        dataStore.saveChatRoom(room);
        return Packet.success(request, null);
    }

    private Packet handleChatSend(Packet request) {
        requireLogin();
        ChatSendRequest payload = (ChatSendRequest) request.getPayload();
        ChatRoom room = dataStore.getChatRoom(payload.getRoomId());
        if (!room.getMemberIds().contains(currentUser.getId())) {
            throw new IllegalStateException("참여 중이 아닌 채팅방입니다: " + room.getRoomId());
        }
        Chat chat = new Chat(currentUser.getId(), LocalDateTime.now(), payload.getContent());
        room.sendChat(chat);
        dataStore.saveChatRoom(room);

        Packet pushPacket = Packet.push(RequestType.CHAT_MESSAGE_PUSH,
                new ChatPushPayload(room.getRoomId(), chat));
        for (String memberId : room.getMemberIds()) {
            if (!memberId.equals(currentUser.getId())) { // 보낸 사람은 응답으로 이미 안다
                sessionRegistry.sendTo(memberId, pushPacket);
            }
        }
        return Packet.success(request, null);
    }

    /** 이 방 안에서 쓸 프로필 이름을 바꾼다. 채팅과 같은 기준으로 참여자만 가능하다. */
    private Packet handleChatRoomSetNickname(Packet request) {
        requireLogin();
        ChatRoomNicknameRequest payload = (ChatRoomNicknameRequest) request.getPayload();
        ChatRoom room = dataStore.getChatRoom(payload.getRoomId());
        if (!room.getMemberIds().contains(currentUser.getId())) {
            throw new IllegalStateException("참여 중이 아닌 채팅방입니다: " + room.getRoomId());
        }
        if (isBlank(payload.getNickname())) {
            throw new IllegalArgumentException("닉네임이 비어 있습니다");
        }
        room.setNickname(currentUser.getId(), payload.getNickname().trim());
        dataStore.saveChatRoom(room);
        return Packet.success(request, null);
    }

    private Packet handleChatRoomList(Packet request) {
        requireLogin();
        return Packet.success(request, dataStore.getAllChatRooms());
    }

    /**
     * 채팅방을 삭제한다 (방장 전용). 공동구매 글에 연결된 방이어도 그대로 지운다 —
     * 그 글은 findLinkedRoom()이 CHATROOM_LIST에서 이 roomId를 못 찾게 되어 연동 전 예전 글과
     * 같은 방식(참여 인원 "?"·채팅방 진입 버튼 숨김)으로 자연히 처리된다.
     */
    private Packet handleChatRoomDelete(Packet request) {
        requireLogin();
        String roomId = (String) request.getPayload();
        ChatRoom room = dataStore.getChatRoom(roomId);
        if (!room.canDelete(currentUser)) {
            throw new IllegalStateException("방장만 채팅방을 삭제할 수 있습니다: " + roomId);
        }
        dataStore.removeChatRoom(roomId);
        return Packet.success(request, null);
    }

    // ── 핸들러 공용 헬퍼 ────────────────────────────────────────────────────

    private void requireLogin() {
        if (currentUser == null) {
            throw new IllegalStateException("로그인이 필요합니다");
        }
    }

    private void requireAdmin() {
        requireLogin();
        if (!currentUser.isAdmin()) {
            throw new IllegalStateException("관리자만 사용할 수 있는 기능입니다");
        }
    }

    /**
     * 게시판마다 .dat 한 줄의 필드 개수가 정해져 있고(parsePost가 그 형식으로만 복원한다),
     * 형식이 다른 게시글이 섞여 저장되면 다음 서버 시작 때 load()가 깨진다. 저장 전에 막는다.
     */
    private void requirePostType(Board board, Post post) {
        Class<? extends Post> expected = Post.class;
        if (board instanceof NoticeBoard) {
            expected = NoticePost.class;
        } else if (board instanceof ComplaintBoard) {
            expected = ComplaintPost.class;
        } else if (board instanceof GroupBuyBoard) {
            expected = GroupBuyPost.class;
        }
        if (post.getClass() != expected) {
            throw new IllegalArgumentException("이 게시판에 맞지 않는 게시글 형식입니다: "
                    + post.getClass().getSimpleName() + " (필요: " + expected.getSimpleName() + ")");
        }
    }

    /**
     * 공동구매 글에 딸리는 채팅방을 만든다 (아직 dataStore에 등록하지는 않는다).
     * 방장 = 글쓴이, 정원 = 글의 maxMembers — "현재 인원수 = 채팅방 참여자 수"(02_requirements §3.1)가
     * 성립하려면 두 값이 같아야 한다. 가입 제한(학번/학과/기숙사)은 걸지 않는다 —
     * 공동구매는 게시판을 볼 수 있는 사람이면 누구나 참여할 수 있어야 한다.
     */
    private ChatRoom createLinkedChatRoom(GroupBuyPost post) {
        ChatRoom room = new ChatRoom(nextRoomId(), currentUser.getId(),
                requireGroupBuyMaxMembers(post.getMaxMembers()));
        room.setName(post.getTitle()); // 방 이름은 글 제목 — 목록/검색에서 어떤 공동구매인지 바로 보이게
        room.getMemberIds().add(currentUser.getId()); // 글쓴이가 방장 겸 첫 참여자
        return room;
    }

    /**
     * 글쓴이가 이미 참여자로 들어가므로 정원이 1이면 만들자마자 가득 차서 아무도 못 들어온다.
     * 무제한(-1)이 아니라면 최소 2명이어야 한다.
     */
    private int requireGroupBuyMaxMembers(int maxMembers) {
        if (maxMembers != -1 && maxMembers < 2) {
            throw new IllegalArgumentException("공동구매 최대 인원은 2명 이상이어야 합니다 (무제한은 -1)");
        }
        return maxMembers;
    }

    private void requireAccess(Board board, String boardKey) {
        if (!board.canAccess(currentUser)) {
            throw new IllegalStateException("접근 권한이 없는 게시판입니다: " + boardKey);
        }
    }

    /** 방장 전용 동작(가입 승인/거절)에서 사용 */
    private ChatRoom requireOwnedRoom(String roomId) {
        ChatRoom room = dataStore.getChatRoom(roomId);
        if (!room.getOwnerId().equals(currentUser.getId())) {
            throw new IllegalStateException("방장만 사용할 수 있는 기능입니다: " + roomId);
        }
        return room;
    }

    /**
     * 업로드된 이름을 그대로 파일명으로 쓰면 두 가지가 깨진다:
     * (1) "../users.dat" 같은 이름으로 엉뚱한 곳에 쓰기, (2) DataFormat의 구분자(| , ^ ; :)가
     * 이름에 들어가면 이 경로를 담은 게시글 .dat 한 줄이 통째로 깨진다. 그래서 화이트리스트로 거른다.
     */
    private static String sanitizeFileName(String name) {
        String bare = name == null ? "" : name;
        int lastSeparator = Math.max(bare.lastIndexOf('/'), bare.lastIndexOf('\\'));
        bare = bare.substring(lastSeparator + 1);
        bare = bare.replaceAll("[^\\w.\\-가-힣]", "_"); // 영숫자/_/./-/한글만 남기고 전부 '_'
        if (bare.isEmpty() || bare.equals(".") || bare.equals("..")) {
            return "attachment";
        }
        return bare;
    }

    /** 저장 이름 "UUID_원본이름"에서 원본 이름만 되돌린다 (UUID에는 '_'가 없어서 첫 '_'가 경계). */
    private static String originalFileName(String storedName) {
        int separator = storedName.indexOf('_');
        return separator >= 0 && separator < storedName.length() - 1
                ? storedName.substring(separator + 1)
                : storedName;
    }

    /** 클라이언트가 보낸 경로에서 파일명만 취해 업로드 폴더 안으로 한정한다 (다른 파일 열람 방지). */
    private Path resolveUpload(String storedPath) {
        if (isBlank(storedPath)) {
            throw new IllegalArgumentException("첨부 경로가 비어 있습니다");
        }
        int lastSeparator = Math.max(storedPath.lastIndexOf('/'), storedPath.lastIndexOf('\\'));
        Path target = UPLOAD_DIR.resolve(storedPath.substring(lastSeparator + 1)).normalize();
        if (!target.startsWith(UPLOAD_DIR) || !Files.isRegularFile(target)) {
            throw new NoSuchElementException("첨부를 찾을 수 없음: " + storedPath);
        }
        return target;
    }

    private Post findPost(Board board, String postId) {
        return board.getPosts().stream()
                .filter(p -> p.getId().equals(postId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없음: " + postId));
    }

    /** 기존 방 id 중 가장 큰 숫자 + 1을 세 자리로 채번 (server/data/chatrooms/001.dat 형식과 동일) */
    private String nextRoomId() {
        int max = 0;
        for (ChatRoom room : dataStore.getAllChatRooms()) {
            try {
                max = Math.max(max, Integer.parseInt(room.getRoomId()));
            } catch (NumberFormatException ignored) {
                // 숫자가 아닌 id는 채번 대상에서 제외
            }
        }
        return String.format("%03d", max + 1);
    }

    /** 로그인 세션 해제 (로그아웃 / 연결 종료 공용) */
    private void clearSession() {
        User user = currentUser;
        if (user != null) {
            sessionRegistry.unregister(user.getId(), this);
            currentUser = null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
