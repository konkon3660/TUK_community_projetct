package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class Chat implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String senderId;
    private final LocalDateTime sentAt;
    private final String content;

    public Chat(String senderId, LocalDateTime sentAt, String content) {
        this.senderId = senderId;
        this.sentAt = sentAt;
        this.content = content;
    }

    public String getSenderId() {
        return senderId;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public String getContent() {
        return content;
    }

    /** 채팅 내용도 댓글과 마찬가지로 '^'와 ';' 두 겹 안쪽에 있어서 반드시 encode해야 한다. */
    public String toDataString() {
        return String.join(DataFormat.SUBOBJECT_DELIM,
                DataFormat.encode(senderId), sentAt.format(DataFormat.DATETIME_FORMATTER),
                DataFormat.encode(content));
    }

    public static Chat fromDataString(String data) {
        String[] f = data.split(Pattern.quote(DataFormat.SUBOBJECT_DELIM), -1);
        return new Chat(DataFormat.decode(f[0]),
                LocalDateTime.parse(f[1], DataFormat.DATETIME_FORMATTER), DataFormat.decode(f[2]));
    }
}
