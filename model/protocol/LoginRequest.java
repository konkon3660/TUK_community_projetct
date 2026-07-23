package model.protocol;

import java.io.Serializable;

public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String password;

    public LoginRequest(String id, String password) {
        this.id = id;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }
}
