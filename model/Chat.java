package model;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class Chat {
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

    public String toDataString() {
        return String.join(DataFormat.SUBOBJECT_DELIM,
                senderId, sentAt.format(DataFormat.DATETIME_FORMATTER), content);
    }

    public static Chat fromDataString(String data) {
        String[] f = data.split(Pattern.quote(DataFormat.SUBOBJECT_DELIM), -1);
        return new Chat(f[0], LocalDateTime.parse(f[1], DataFormat.DATETIME_FORMATTER), f[2]);
    }
}
