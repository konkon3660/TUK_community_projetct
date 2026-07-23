package model.boards;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    /** 현재 인원수는 항상 연결된 채팅방(chatRoomId)의 참여자 수와 동일해야 함 */
    public int getCurrentMemberCount(ChatRoom linkedRoom) {
        throw new UnsupportedOperationException("TODO: 구현 필요");
    }

    @Override
    public String toDataString() {
        String hashtagsEncoded = String.join(DataFormat.LIST_DELIM, hashtags);
        return String.join(DataFormat.FIELD_DELIM,
                baseDataString(), String.valueOf(maxMembers), chatRoomId, hashtagsEncoded);
    }

    public static GroupBuyPost fromDataString(String line) {
        String[] f = splitFields(line);
        List<String> hashtags = f[10].isEmpty()
                ? new ArrayList<>()
                : Arrays.stream(f[10].split(DataFormat.LIST_DELIM)).collect(Collectors.toList());
        GroupBuyPost post = new GroupBuyPost(f[0], f[1], f[2], f[3], emptyToNull(f[4]), emptyToNull(f[5]),
                LocalDateTime.parse(f[6], DataFormat.DATETIME_FORMATTER),
                Integer.parseInt(f[8]), f[9], hashtags);
        post.getComments().addAll(parseComments(f[7]));
        return post;
    }
}
