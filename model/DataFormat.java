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

    /** 이스케이프 시작 문자. 값 안에 이 문자가 있으면 두 번 겹쳐서 저장한다. */
    private static final char ESCAPE = '\\';

    /**
     * 값 안에 구분자나 줄바꿈이 들어가도 저장 포맷이 깨지지 않도록 치환한다.
     *
     * <p><b>사용자가 입력한 말단 값에만 씌운다.</b> 저장 포맷은 중첩 구조라서
     * (게시글 안에 댓글, 채팅방 안에 채팅), 구조를 만드는 join이 넣는 구분자까지 치환하면
     * 오히려 구조가 사라진다. 말단에 구분자가 남아있지 않기만 하면 모든 단계의 split이
     * 자동으로 안전해진다.
     *
     * <p>줄바꿈까지 치환하는 이유: 파일은 <b>한 줄 = 한 레코드</b>인데 본문은 여러 줄 입력
     * (JTextArea)이라, 그대로 두면 게시글 하나가 파일 두 줄이 되어 다음 서버 시작 때
     * load()가 실패한다. 실제로 구분자보다 이쪽이 훨씬 자주 터진다.
     *
     * <p>null은 빈 문자열로 본다 (첨부 없음 = 빈 문자열이라는 기존 규칙과 같다).
     */
    public static String encode(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\") // 이스케이프 문자 자신을 반드시 가장 먼저 (뒤에 하면 방금 넣은 걸 또 바꾼다)
                .replace(FIELD_DELIM, "\\p")
                .replace(SUBOBJECT_DELIM, "\\c")
                .replace(SUBLIST_DELIM, "\\s")
                .replace(LIST_DELIM, "\\l")
                .replace(MAP_ENTRY_DELIM, "\\m")
                .replace("\r", "") // 붙여넣기로 들어온 CRLF의 CR은 버린다
                .replace("\n", "\\n");
    }

    /**
     * encode()로 저장된 값을 원래대로 되돌린다.
     *
     * <p>왼쪽부터 한 번만 훑는다 — <b>replace를 이어 붙여 구현하면 안 된다.</b>
     * 그렇게 하면 사용자가 실제로 입력한 역슬래시(저장된 모습은 {@code \\p})를
     * {@code |}로 잘못 복원한다. 이스케이프 구현에서 가장 흔한 버그다.
     *
     * <p>역슬래시가 없는 문자열은 그대로 돌려주므로, 이스케이프 도입 전에 저장된
     * 기존 .dat 파일도 변환 없이 그대로 읽힌다.
     */
    public static String decode(String value) {
        if (value == null || value.indexOf(ESCAPE) < 0) {
            return value; // 흔한 경우 — 훑을 것도 없다
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != ESCAPE || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case 'p': out.append(FIELD_DELIM); break;
                case 'c': out.append(SUBOBJECT_DELIM); break;
                case 's': out.append(SUBLIST_DELIM); break;
                case 'l': out.append(LIST_DELIM); break;
                case 'm': out.append(MAP_ENTRY_DELIM); break;
                case 'n': out.append('\n'); break;
                case '\\': out.append(ESCAPE); break;
                // 우리가 만든 적 없는 조합은 손대지 않고 그대로 둔다 (손으로 고친 파일 대비)
                default: out.append(ESCAPE).append(next); break;
            }
        }
        return out.toString();
    }

    private DataFormat() {
    }
}
