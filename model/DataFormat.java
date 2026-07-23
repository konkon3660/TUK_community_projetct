package model;

import java.time.format.DateTimeFormatter;

/**
 * 모든 모델 클래스가 .dat 파일을 읽고 쓸 때 공통으로 사용하는 구분자/포맷 상수.
 * 여기 값을 벗어난 별도의 구분자를 사용하지 말 것 (직렬화 포맷이 클래스마다 달라지는 것을 방지).
 */
public final class DataFormat {
    /** 한 레코드 안에서 필드와 필드를 구분 */
    public static final String FIELD_DELIM = "|";
    /** 리스트 형태 필드(해시태그, 학과 목록, 멤버 id 목록 등) 내부 원소 구분 */
    public static final String LIST_DELIM = ",";
    /** 중첩 객체(댓글, 채팅 메시지) 하나의 내부 필드 구분 */
    public static final String SUBOBJECT_DELIM = "^";
    /** 중첩 객체가 여러 개일 때 그 객체들 사이의 구분 */
    public static final String SUBLIST_DELIM = ";";
    /** Map 형태 필드(예: 채팅방 가입 신청 메세지)의 key:value 구분 */
    public static final String MAP_ENTRY_DELIM = ":";

    public static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

    private DataFormat() {
    }
}
