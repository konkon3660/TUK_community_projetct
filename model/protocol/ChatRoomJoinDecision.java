package model.protocol;

import java.io.Serializable;

public class ChatRoomJoinDecision implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String roomId;
    private final String userId;

    public ChatRoomJoinDecision(String roomId, String userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getUserId() {
        return userId;
    }
}
