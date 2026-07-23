package client.CT;

import model.protocol.Packet;

/**
 * 서버가 요청 없이 먼저 보내는 푸시 패킷(새 채팅, 새 공지 등)을 받는 콜백.
 * GUI 쪽에서 구현해서 ServerConnection.setPushListener로 등록한다.
 */
public interface PushListener {
    void onPush(Packet packet);
}
