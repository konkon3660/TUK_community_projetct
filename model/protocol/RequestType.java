package model.protocol;

/**
 * 클라이언트-서버 간 오가는 모든 Packet의 종류. 각 상수에 요청/응답 payload 타입을 고정해두었다.
 * 새 기능이 필요하면 이 enum에 상수를 추가하고 documents/protocol.md의 표도 함께 갱신할 것 —
 * ClientHandler에서 임의로 새 타입을 만들어 쓰지 않는다.
 */
public enum RequestType {
    /** 요청 payload: {@link model.protocol.LoginRequest}. 응답 payload: {@code model.User} */
    LOGIN,

    /** 요청 payload: {@code model.User}(신규 가입 정보). 응답 payload: 없음 */
    REGISTER,

    /** 요청 payload: 없음. 응답 payload: 없음 */
    LOGOUT,

    /** 요청 payload: {@code model.User}(수정 반영된 전체 정보), 관리자 전용. 응답 payload: 없음 */
    USER_UPDATE,

    /** 요청 payload: {@code String}(boardKey). 응답 payload: {@code List<model.Post>} */
    POST_LIST,

    /** 요청 payload: {@link model.protocol.PostCreateOrUpdateRequest}. 응답 payload: {@code model.Post}(저장된 결과) */
    POST_CREATE,

    /** 요청 payload: {@link model.protocol.PostCreateOrUpdateRequest}. 응답 payload: 없음 */
    POST_UPDATE,

    /** 요청 payload: {@link model.protocol.PostDeleteRequest}. 응답 payload: 없음 */
    POST_DELETE,

    /** 요청 payload: {@link model.protocol.CommentAddRequest}. 응답 payload: 없음 */
    COMMENT_ADD,

    /** 요청 payload: {@link model.protocol.CommentDeleteRequest}. 응답 payload: 없음 */
    COMMENT_DELETE,

    /** 요청 payload: {@code model.ChatRoom}(생성할 방의 초기값). 응답 payload: {@code model.ChatRoom}(roomId 채번 결과) */
    CHATROOM_CREATE,

    /** 요청 payload: {@link model.protocol.ChatRoomJoinRequest}. 응답 payload: 없음 */
    CHATROOM_JOIN_REQUEST,

    /** 요청 payload: {@link model.protocol.ChatRoomJoinDecision}, 방장 전용. 응답 payload: 없음 */
    CHATROOM_JOIN_APPROVE,

    /** 요청 payload: {@link model.protocol.ChatRoomJoinDecision}, 방장 전용. 응답 payload: 없음 */
    CHATROOM_JOIN_REJECT,

    /** 요청 payload: {@link model.protocol.ChatSendRequest}. 응답 payload: 없음(전송 성공 여부만) */
    CHAT_SEND,

    /** 서버 전용 푸시(요청 없음). payload: {@link model.protocol.ChatPushPayload}(roomId + 새 Chat) */
    CHAT_MESSAGE_PUSH,

    /** 서버 전용 푸시(요청 없음). payload: {@code model.NoticePost}(새로 등록된 공지) */
    NOTICE_PUSH,

    /** 연결 종료 통지. 요청/응답 payload 없음 */
    DISCONNECT
}
