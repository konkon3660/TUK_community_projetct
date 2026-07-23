package model.protocol;

import java.io.Serializable;
import java.util.UUID;

/**
 * 클라이언트-서버가 소켓으로 주고받는 모든 메시지의 봉투. 생성자를 직접 열어두지 않고
 * 아래 정적 팩토리 4개로만 만들 수 있게 해서 "요청/성공응답/실패응답/푸시" 4가지 형태
 * 외의 조합이 나오지 않도록 한다.
 */
public class Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    private final RequestType type;
    private final String requestId; // 클라이언트가 요청 생성 시 채번, 응답에 그대로 echo. 푸시는 null.
    private final ResponseStatus status; // 요청/푸시 패킷이면 null
    private final String errorMessage; // status == ERROR 일 때만 값 있음
    private final Object payload; // 타입별 실제 모양은 RequestType의 Javadoc 참고

    private Packet(RequestType type, String requestId, ResponseStatus status,
                    String errorMessage, Object payload) {
        this.type = type;
        this.requestId = requestId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.payload = payload;
    }

    /** 클라이언트 -> 서버 요청 생성 (requestId 자동 채번) */
    public static Packet request(RequestType type, Object payload) {
        return new Packet(type, UUID.randomUUID().toString(), null, null, payload);
    }

    /** 서버 -> 클라이언트 성공 응답 (원래 요청의 requestId를 그대로 echo) */
    public static Packet success(Packet request, Object payload) {
        return new Packet(request.getType(), request.getRequestId(), ResponseStatus.OK, null, payload);
    }

    /** 서버 -> 클라이언트 실패 응답 */
    public static Packet error(Packet request, String errorMessage) {
        return new Packet(request.getType(), request.getRequestId(), ResponseStatus.ERROR, errorMessage, null);
    }

    /** 서버가 요청 없이 먼저 보내는 푸시 메시지 (예: 다른 사람이 보낸 채팅, 새 공지) */
    public static Packet push(RequestType type, Object payload) {
        return new Packet(type, null, null, null, payload);
    }

    public RequestType getType() {
        return type;
    }

    public String getRequestId() {
        return requestId;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Object getPayload() {
        return payload;
    }

    /** requestId가 없으면(=서버가 먼저 보낸 것이면) 푸시 메시지 */
    public boolean isPush() {
        return requestId == null;
    }
}
