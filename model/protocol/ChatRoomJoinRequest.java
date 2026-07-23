package model.protocol;

import java.io.Serializable;

public class ChatRoomJoinRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String roomId;
    private final String message;

    public ChatRoomJoinRequest(String roomId, String message) {
        this.roomId = roomId;
        this.message = message;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getMessage() {
        return message;
    }
}
