package server.CT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import model.User;
import model.protocol.Packet;

/**
 * 지금 로그인해 있는 세션(userId -> 그 클라이언트를 담당하는 ClientHandler)을 모아둔다.
 * 채팅/공지처럼 서버가 먼저 보내는 푸시의 "대상을 찾는" 용도로만 쓰고, 실제 전송은
 * ClientHandler.sendPacket()을 그대로 재사용한다. 서버당 하나만 만들어 모든
 * ClientHandler가 공유한다.
 */
public class SessionRegistry {
    private final Map<String, ClientHandler> sessions = new ConcurrentHashMap<>();

    /** 같은 학번으로 다시 로그인하면 마지막 연결이 그 유저의 세션이 된다. */
    public void register(String userId, ClientHandler handler) {
        sessions.put(userId, handler);
    }

    /** 로그아웃하거나 연결이 끊길 때 호출. 이미 다른 연결로 교체된 세션은 건드리지 않는다. */
    public void unregister(String userId, ClientHandler handler) {
        sessions.remove(userId, handler);
    }

    /** 대상이 접속 중이 아니면 아무 일도 하지 않는다 (오프라인은 정상 상황). */
    public void sendTo(String userId, Packet packet) {
        ClientHandler handler = sessions.get(userId);
        if (handler != null) {
            handler.sendPacket(packet);
        }
    }

    /** 조건에 맞는 접속자 전원에게 푸시 (예: 이 공지가 보여야 하는 유저들). */
    public void sendToAll(Packet packet, Predicate<User> filter) {
        for (ClientHandler handler : sessions.values()) {
            User user = handler.getCurrentUser();
            if (user != null && filter.test(user)) {
                handler.sendPacket(packet);
            }
        }
    }
}
