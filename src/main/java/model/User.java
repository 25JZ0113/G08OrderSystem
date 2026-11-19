package model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * ユーザー(USERS)。ログイン中のユーザーとしてセッションに入れる
 * パスワード(のハッシュ値)はセッションに持たせないので、このクラスには入れない
 * 登録日時(DATE 型)は Timestamp で持つ
 */
public class User implements Serializable {

    /** 権限 */
    public static final String ROLE_ADMIN = "管理者";
    public static final String ROLE_STAFF = "社員";
    /** 利用状態 */
    public static final String STATUS_ACTIVE  = "利用中";
    public static final String STATUS_STOPPED = "利用停止";

    private int    id;
    private String loginId;
    private String name;
    private String role;
    private String status;
    private Timestamp createdDatetime;

    public User() {}

    public User(int id, String loginId, String name, String role, String status) {
        this.id      = id;
        this.loginId = loginId;
        this.name    = name;
        this.role    = role;
        this.status  = status;
    }

    /** 管理者か */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(Timestamp createdDatetime) {
        this.createdDatetime = createdDatetime;
    }
}
