package model.protocol;

import java.io.Serializable;

/** CHATROOM_SET_NICKNAME의 요청 payload: 어느 방(roomId)에서 쓸 닉네임인지. */
public class ChatRoomNicknameRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String roomId;
    private final String nickname;

    public ChatRoomNicknameRequest(String roomId, String nickname) {
        this.roomId = roomId;
        this.nickname = nickname;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getNickname() {
        return nickname;
    }
}
