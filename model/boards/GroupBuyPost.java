package model.boards;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import model.ChatRoom;
import model.DataFormat;

public class GroupBuyPost extends Post {
    private static final long serialVersionUID = 1L;

    // 필드 순서(라인 내 토큰 인덱스, 0~7은 Post 공통 필드): 8=maxMembers, 9=chatRoomId, 10=hashtags
    private int maxMembers;
    private String chatRoomId;
    private final List<String> hashtags;

    public GroupBuyPost(String id, String title, String authorId, String content,
                         String filePath, String imagePath, LocalDateTime createdAt,
                         int maxMembers, String chatRoomId, List<String> hashtags) {
        super(id, title, authorId, content, filePath, imagePath, createdAt);
        this.maxMembers = maxMembers;
        this.chatRoomId = chatRoomId;
        this.hashtags = hashtags;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    /** 연결된 채팅방을 못 받았을 때 돌려주는 값. "0명"과 "아직 모름"을 구분하기 위해 음수를 쓴다. */
    public static final int UNKNOWN_MEMBER_COUNT = -1;

    /**
     * 현재 인원수는 항상 연결된 채팅방(chatRoomId)의 참여자 수와 동일해야 함.
     * 별도 카운터를 두지 않고 방을 그대로 세는 이유는 두 값이 어긋날 여지를 없애기 위해서다.
     * linkedRoom이 null이면(방 목록을 못 받았거나 chatRoomId가 비어있는 예전 글) UNKNOWN_MEMBER_COUNT.
     */
    public int getCurrentMemberCount(ChatRoom linkedRoom) {
        return linkedRoom == null ? UNKNOWN_MEMBER_COUNT : linkedRoom.getMemberIds().size();
    }

    @Override
    public String toDataString() {
        // 해시태그는 쉼표로 이어붙이므로 태그 하나하나를 encode한다 (태그에 쉼표가 들어가도 안전하게).
        String hashtagsEncoded = hashtags.stream()
                .map(DataFormat::encode)
                .collect(Collectors.joining(DataFormat.LIST_DELIM));
        return String.join(DataFormat.FIELD_DELIM,
                baseDataString(), String.valueOf(maxMembers),
                DataFormat.encode(chatRoomId), hashtagsEncoded);
    }

    public static GroupBuyPost fromDataString(String line) {
        String[] f = splitFields(line);
        List<String> hashtags = f[10].isEmpty()
                ? new ArrayList<>()
                : Arrays.stream(f[10].split(Pattern.quote(DataFormat.LIST_DELIM)))
                        .map(DataFormat::decode)
                        .collect(Collectors.toList());
        GroupBuyPost post = new GroupBuyPost(f[0], f[1], f[2], f[3], emptyToNull(f[4]), emptyToNull(f[5]),
                LocalDateTime.parse(f[6], DataFormat.DATETIME_FORMATTER),
                Integer.parseInt(f[8]), f[9], hashtags);
        post.getComments().addAll(parseComments(f[7]));
        return post;
    }
}
