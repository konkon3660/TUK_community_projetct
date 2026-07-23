package server.CT;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.EnumSet;
import java.util.Set;

import model.User;
import model.protocol.Packet;
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
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser; // 로그인 성공 시 세팅 (세팅하는 로직은 handleLogin의 TODO)

    public ClientHandler(Socket socket, DataStore dataStore) {
        this.socket = socket;
        this.dataStore = dataStore;
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

    // ── 아래는 실제 업무 로직 자리 (TODO: 구현 필요) ─────────────────────────
    // model/ 패키지의 User/Board/Post/ChatRoom 메서드를 그대로 호출해서 채우면 된다.
    // 로그인한 유저를 찾거나 게시판 인스턴스를 찾는 레지스트리는 이번 범위 밖이므로
    // 별도로 설계된 뒤 연결한다.

    private Packet handleLogin(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleRegister(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleLogout(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleUserUpdate(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handlePostList(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handlePostCreate(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handlePostUpdate(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handlePostDelete(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleCommentAdd(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleCommentDelete(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleChatRoomCreate(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleChatRoomJoinRequest(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleChatRoomJoinApprove(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleChatRoomJoinReject(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    private Packet handleChatSend(Packet request) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }
}
