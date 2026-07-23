package model.protocol;

import java.io.Serializable;

import model.Chat;

/** CHAT_MESSAGE_PUSH 전용 payload: 어느 방에 어떤 채팅이 새로 도착했는지 */
public class ChatPushPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String roomId;
    private final Chat chat;

    public ChatPushPayload(String roomId, Chat chat) {
        this.roomId = roomId;
        this.chat = chat;
    }

    public String getRoomId() {
        return roomId;
    }

    public Chat getChat() {
        return chat;
    }
}
