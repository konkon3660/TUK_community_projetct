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
    private String name = ""; // 방 이름 (검색용). 이름 도입 전에 저장된 방은 빈 문자열
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
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

    /**
     * 자격 제한(입학년도/학과/기숙사)이 하나도 없으면 방장 승인 없이 바로 입장할 수 있는 방이다.
     * 정원은 자격 심사가 아니라 수용 한계이므로 여기서 보지 않는다 (가득 차면 canJoin/join이 막는다).
     * 이런 방은 requestJoin(승인 대기) 대신 join(즉시 참여)으로 처리한다.
     */
    public boolean isOpenJoin() {
        return admissionYearLimit == null && departmentLimit.isEmpty() && !dormOnlyLimit;
    }

    /** 방장 승인 없이 바로 참여시킨다 (자격 제한 없는 방 전용). 정원·중복 참여는 canJoin이 막는다. */
    public void join(User user) {
        if (!canJoin(user)) {
            throw new IllegalStateException("가입 조건을 만족하지 않습니다: " + roomId);
        }
        pendingJoinRequests.remove(user.getId()); // 혹시 남아있던 신청은 정리
        memberIds.add(user.getId());
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

    /** 채팅방 삭제는 방장만 할 수 있다. */
    public boolean canDelete(User requester) {
        return requester.getId().equals(ownerId);
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
        String deptEncoded = encodeList(departmentLimit);
        String memberEncoded = encodeList(memberIds);
        String chatsEncoded = chats.stream()
                .map(Chat::toDataString)
                .collect(Collectors.joining(DataFormat.SUBLIST_DELIM));
        String pendingEncoded = encodeMap(pendingJoinRequests);
        String nicknamesEncoded = encodeMap(nicknames);
        // name은 나중에 추가된 필드라 맨 뒤에 붙인다 — 필드 순서를 중간에서 바꾸면 기존 .dat이 깨진다.
        return String.join(DataFormat.FIELD_DELIM,
                DataFormat.encode(roomId), DataFormat.encode(ownerId), String.valueOf(maxMembers),
                admissionYearLimit == null ? "" : String.valueOf(admissionYearLimit),
                deptEncoded, String.valueOf(dormOnlyLimit), String.valueOf(inviteBypassesLimit),
                memberEncoded, chatsEncoded, pendingEncoded, nicknamesEncoded,
                DataFormat.encode(name));
    }

    /** 리스트 필드는 쉼표로 이어붙이므로 원소 하나하나를 encode한다. */
    private static String encodeList(List<String> values) {
        return values.stream().map(DataFormat::encode).collect(Collectors.joining(DataFormat.LIST_DELIM));
    }

    /** "키:값"을 쉼표로 이어붙인다. 키와 값을 모두 encode하므로 가입 신청 메세지나 닉네임에
     *  콜론·쉼표가 들어가도 안전하다 (예: "3시:30분에 가능해요, 늦으면 말씀해주세요"). */
    private static String encodeMap(Map<String, String> map) {
        return map.entrySet().stream()
                .map(e -> DataFormat.encode(e.getKey()) + DataFormat.MAP_ENTRY_DELIM
                        + DataFormat.encode(e.getValue()))
                .collect(Collectors.joining(DataFormat.LIST_DELIM));
    }

    private static List<String> decodeList(String encoded) {
        return Arrays.stream(encoded.split(Pattern.quote(DataFormat.LIST_DELIM)))
                .map(DataFormat::decode)
                .collect(Collectors.toList());
    }

    private static void decodeMapInto(String encoded, Map<String, String> target) {
        for (String entry : encoded.split(Pattern.quote(DataFormat.LIST_DELIM))) {
            String[] kv = entry.split(Pattern.quote(DataFormat.MAP_ENTRY_DELIM), 2);
            target.put(DataFormat.decode(kv[0]), DataFormat.decode(kv[1]));
        }
    }

    public static ChatRoom fromDataString(String line) {
        String[] f = line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
        ChatRoom room = new ChatRoom(DataFormat.decode(f[0]), DataFormat.decode(f[1]),
                Integer.parseInt(f[2]));
        room.admissionYearLimit = f[3].isEmpty() ? null : Integer.parseInt(f[3]);
        if (!f[4].isEmpty()) {
            room.departmentLimit.addAll(decodeList(f[4]));
        }
        room.dormOnlyLimit = Boolean.parseBoolean(f[5]);
        room.inviteBypassesLimit = Boolean.parseBoolean(f[6]);
        if (!f[7].isEmpty()) {
            room.memberIds.addAll(decodeList(f[7]));
        }
        if (!f[8].isEmpty()) {
            for (String part : f[8].split(Pattern.quote(DataFormat.SUBLIST_DELIM))) {
                room.chats.add(Chat.fromDataString(part));
            }
        }
        if (!f[9].isEmpty()) {
            decodeMapInto(f[9], room.pendingJoinRequests);
        }
        if (!f[10].isEmpty()) {
            decodeMapInto(f[10], room.nicknames);
        }
        // 이름 필드 도입 전에 저장된 11필드 .dat도 그대로 읽혀야 한다 — 없으면 빈 이름.
        if (f.length > 11) {
            room.name = DataFormat.decode(f[11]);
        }
        return room;
    }
}
