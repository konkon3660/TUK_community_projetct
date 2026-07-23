package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String roomId;
    private final String ownerId;
    private final List<String> memberIds;
    private final List<Chat> chats;
    private int maxMembers; // -1 = 무제한
    private Integer admissionYearLimit; // null = 제한 없음, 값이 있으면 학번 앞 4자리와 일치해야 가입 가능
    private final List<String> departmentLimit; // 비어있으면 학과 제한 없음
    private boolean dormOnlyLimit;
    private boolean inviteBypassesLimit; // true면 초대로 들어온 인원은 위 제한들을 무시함
    private final Map<String, String> pendingJoinRequests; // userId -> 가입지원 메세지
    private final Map<String, String> nicknames; // userId -> 이 채팅방 안에서의 프로필 이름

    public ChatRoom(String roomId, String ownerId, int maxMembers) {
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.maxMembers = maxMembers;
        this.memberIds = new ArrayList<>();
        this.chats = new ArrayList<>();
        this.departmentLimit = new ArrayList<>();
        this.pendingJoinRequests = new LinkedHashMap<>();
        this.nicknames = new HashMap<>();
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public Integer getAdmissionYearLimit() {
        return admissionYearLimit;
    }

    public void setAdmissionYearLimit(Integer admissionYearLimit) {
        this.admissionYearLimit = admissionYearLimit;
    }

    public List<String> getDepartmentLimit() {
        return departmentLimit;
    }

    public boolean isDormOnlyLimit() {
        return dormOnlyLimit;
    }

    public void setDormOnlyLimit(boolean dormOnlyLimit) {
        this.dormOnlyLimit = dormOnlyLimit;
    }

    public boolean isInviteBypassesLimit() {
        return inviteBypassesLimit;
    }

    public void setInviteBypassesLimit(boolean inviteBypassesLimit) {
        this.inviteBypassesLimit = inviteBypassesLimit;
    }

    public Map<String, String> getPendingJoinRequests() {
        return pendingJoinRequests;
    }

    /**
     * 입학년도/학과/기숙사 제한 + 정원을 검사해서 가입 가능 여부를 판단.
     * inviteBypassesLimit은 초대 기능이 아직 없어서 판정에 쓰지 않는다 (값 보관만).
     */
    public boolean canJoin(User user) {
        if (memberIds.contains(user.getId())) {
            return false;
        }
        if (isFull()) {
            return false;
        }
        if (admissionYearLimit != null && !matchesAdmissionYear(user.getId())) {
            return false;
        }
        if (!departmentLimit.isEmpty() && !departmentLimit.contains(user.getDepartment())) {
            return false;
        }
        return !dormOnlyLimit || user.isDormitory();
    }

    /** 가입지원 메세지와 함께 신청을 등록 (방장이 approveJoin/rejectJoin으로 처리) */
    public void requestJoin(User user, String applicationMessage) {
        if (memberIds.contains(user.getId())) {
            throw new IllegalStateException("이미 참여 중인 채팅방입니다: " + roomId);
        }
        if (!canJoin(user)) {
            throw new IllegalStateException("가입 조건을 만족하지 않습니다: " + roomId);
        }
        pendingJoinRequests.put(user.getId(), applicationMessage);
    }

    public void approveJoin(String userId) {
        if (!pendingJoinRequests.containsKey(userId)) {
            throw new NoSuchElementException("가입 신청 내역이 없습니다: " + userId);
        }
        if (isFull()) {
            throw new IllegalStateException("정원이 가득 찼습니다: " + roomId);
        }
        pendingJoinRequests.remove(userId);
        memberIds.add(userId);
    }

    public void rejectJoin(String userId) {
        if (pendingJoinRequests.remove(userId) == null) {
            throw new NoSuchElementException("가입 신청 내역이 없습니다: " + userId);
        }
    }

    private boolean isFull() {
        return maxMembers != -1 && memberIds.size() >= maxMembers;
    }

    /** 학번 앞 4자리(입학년도)가 제한값과 같은지. 학번이 비정상이면 가입 불가로 본다. */
    private boolean matchesAdmissionYear(String userId) {
        if (userId.length() < 4) {
            return false;
        }
        try {
            return Integer.parseInt(userId.substring(0, 4)) == admissionYearLimit;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void setNickname(String userId, String nickname) {
        nicknames.put(userId, nickname);
    }

    public String getNickname(String userId) {
        return nicknames.get(userId);
    }

    public void sendChat(Chat chat) {
        chats.add(chat);
    }

    public String toDataString() {
        String deptEncoded = String.join(DataFormat.LIST_DELIM, departmentLimit);
        String memberEncoded = String.join(DataFormat.LIST_DELIM, memberIds);
        String chatsEncoded = chats.stream()
                .map(Chat::toDataString)
                .collect(Collectors.joining(DataFormat.SUBLIST_DELIM));
        String pendingEncoded = pendingJoinRequests.entrySet().stream()
                .map(e -> e.getKey() + DataFormat.MAP_ENTRY_DELIM + e.getValue())
                .collect(Collectors.joining(DataFormat.LIST_DELIM));
        String nicknamesEncoded = nicknames.entrySet().stream()
                .map(e -> e.getKey() + DataFormat.MAP_ENTRY_DELIM + e.getValue())
                .collect(Collectors.joining(DataFormat.LIST_DELIM));
        return String.join(DataFormat.FIELD_DELIM,
                roomId, ownerId, String.valueOf(maxMembers),
                admissionYearLimit == null ? "" : String.valueOf(admissionYearLimit),
                deptEncoded, String.valueOf(dormOnlyLimit), String.valueOf(inviteBypassesLimit),
                memberEncoded, chatsEncoded, pendingEncoded, nicknamesEncoded);
    }

    public static ChatRoom fromDataString(String line) {
        String[] f = line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
        ChatRoom room = new ChatRoom(f[0], f[1], Integer.parseInt(f[2]));
        room.admissionYearLimit = f[3].isEmpty() ? null : Integer.parseInt(f[3]);
        if (!f[4].isEmpty()) {
            room.departmentLimit.addAll(Arrays.asList(f[4].split(DataFormat.LIST_DELIM)));
        }
        room.dormOnlyLimit = Boolean.parseBoolean(f[5]);
        room.inviteBypassesLimit = Boolean.parseBoolean(f[6]);
        if (!f[7].isEmpty()) {
            room.memberIds.addAll(Arrays.asList(f[7].split(DataFormat.LIST_DELIM)));
        }
        if (!f[8].isEmpty()) {
            for (String part : f[8].split(Pattern.quote(DataFormat.SUBLIST_DELIM))) {
                room.chats.add(Chat.fromDataString(part));
            }
        }
        if (!f[9].isEmpty()) {
            for (String entry : f[9].split(DataFormat.LIST_DELIM)) {
                String[] kv = entry.split(DataFormat.MAP_ENTRY_DELIM, 2);
                room.pendingJoinRequests.put(kv[0], kv[1]);
            }
        }
        if (!f[10].isEmpty()) {
            for (String entry : f[10].split(DataFormat.LIST_DELIM)) {
                String[] kv = entry.split(DataFormat.MAP_ENTRY_DELIM, 2);
                room.nicknames.put(kv[0], kv[1]);
            }
        }
        return room;
    }
}
