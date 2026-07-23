package model.protocol;

/**
 * 게시판을 가리키는 boardKey 문자열 상수. 5개 고정 게시판만 여기 있고,
 * 학과별 게시판(DepartmentBoard)의 boardKey는 별도 상수 없이 User.getDepartment()와
 * 정확히 같은 문자열을 그대로 사용한다 (학과명은 데이터이지 코드가 아니므로).
 */
public final class BoardKey {
    public static final String FREE = "free";
    public static final String GROUP_BUY = "groupbuy";
    public static final String DORM = "dorm";
    public static final String NOTICE = "notice";
    public static final String COMPLAINT = "complaint";

    private BoardKey() {
    }
}
