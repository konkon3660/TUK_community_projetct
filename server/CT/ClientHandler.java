package server.CT;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import model.Chat;
import model.ChatRoom;
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
import model.protocol.ChatSendRequest;
import model.protocol.CommentAddRequest;
import model.protocol.CommentDeleteRequest;
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
    private static final Set<RequestType> SYNCHRONIZED_TYPES = EnumSet.of(
            RequestType.REGISTER, RequestType.USER_UPDATE,
            RequestType.POST_CREATE, RequestType.POST_UPDATE, RequestType.POST_DELETE,
            RequestType.COMMENT_ADD, RequestType.COMMENT_DELETE,
            RequestType.CHATROOM_CREATE, RequestType.CHATROOM_JOIN_REQUEST,
            RequestType.CHATROOM_JOIN_APPROVE, RequestType.CHATROOM_JOIN_REJECT,
            RequestType.CHAT_SEND);

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
            case CHATROOM_LIST:
                return handleChatRoomList(request);
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

        board.addPost(post);
        board.save();

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
            GroupBuyPost incomingGroupBuy = (GroupBuyPost) incoming;
            storedGroupBuy.setMaxMembers(incomingGroupBuy.getMaxMembers());
            storedGroupBuy.setChatRoomId(incomingGroupBuy.getChatRoomId());
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

    private Packet handleChatRoomCreate(Packet request) {
        requireLogin();
        ChatRoom template = (ChatRoom) request.getPayload();
        // roomId는 클라이언트가 보낸 값을 쓰지 않고 서버가 채번한다 (001, 002, ... 형식).
        ChatRoom room = new ChatRoom(nextRoomId(), currentUser.getId(), template.getMaxMembers());
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

    private Packet handleChatRoomList(Packet request) {
        requireLogin();
        return Packet.success(request, dataStore.getAllChatRooms());
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
