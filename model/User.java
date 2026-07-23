package model;

import java.io.Serializable;
import java.util.regex.Pattern;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

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

    /** 학번/학과/비밀번호는 사용자가 입력한 값이라 encode한다 (특히 비밀번호에 '|'가 들어갈 수 있다). */
    public String toDataString() {
        return String.join(DataFormat.FIELD_DELIM,
                DataFormat.encode(id), DataFormat.encode(department), String.valueOf(dormitory),
                DataFormat.encode(password), String.valueOf(admin));
    }

    public static User fromDataString(String line) {
        String[] f = line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
        return new User(DataFormat.decode(f[0]), DataFormat.decode(f[1]), Boolean.parseBoolean(f[2]),
                DataFormat.decode(f[3]), Boolean.parseBoolean(f[4]));
    }
}
