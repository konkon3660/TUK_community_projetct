package model.protocol;

import java.io.Serializable;

public class ChatSendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String roomId;
    private final String content;

    public ChatSendRequest(String roomId, String content) {
        this.roomId = roomId;
        this.content = content;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getContent() {
        return content;
    }
}
