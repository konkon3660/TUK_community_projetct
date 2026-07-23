package model;

import java.util.regex.Pattern;

public class User {
    private final String id;
    private String department;
    private boolean dormitory;
    private String password;
    private final boolean admin;

    public User(String id, String department, boolean dormitory, String password, boolean admin) {
        this.id = id;
        this.department = department;
        this.dormitory = dormitory;
        this.password = password;
        this.admin = admin;
    }

    public String getId() {
        return id;
    }

    public String getDepartment() {
        return department;
    }

    public boolean isDormitory() {
        return dormitory;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdmin() {
        return admin;
    }

    // 아래 세터들은 관리자만 호출해야 함 (전과/기숙사 입퇴실/비밀번호 재설정). 권한 검증은 호출부 책임.
    public void setDepartment(String department) {
        this.department = department;
    }

    public void setDormitory(boolean dormitory) {
        this.dormitory = dormitory;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String toDataString() {
        return String.join(DataFormat.FIELD_DELIM,
                id, department, String.valueOf(dormitory), password, String.valueOf(admin));
    }

    public static User fromDataString(String line) {
        String[] f = line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
        return new User(f[0], f[1], Boolean.parseBoolean(f[2]), f[3], Boolean.parseBoolean(f[4]));
    }
}
