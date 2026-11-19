package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.User;
import db.DBManager;
import util.PasswordUtil;

/**
 * ユーザー(USERS)へのアクセス
 */
public class UserDAO {

    /**
     * A-01 ログイン:ログイン ID とパスワードが正しく、利用状態が「利用中」のユーザーを返す
     * ID かパスワードが違う・利用停止のときは null(どれが違うかは画面に出さない)
     */
    public User login(String loginId, String password) throws SQLException {
        // 別名に uid は使えない(UID は Oracle の予約語)
        String sql =
                """
                SELECT
                    u.id       AS uuser_id,
                    u.login_id AS ulogin_id,
                    u.password AS upassword,
                    u.name     AS uname,
                    u.role     AS urole,
                    u.status   AS ustatus,
                    u.created_datetime AS ucreated_datetime
                FROM
                    users u
                WHERE u.login_id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                if (!PasswordUtil.matches(password, rs.getString("upassword"))) {   // BCrypt で照合する
                    return null;
                }
                if (!User.STATUS_ACTIVE.equals(rs.getString("ustatus"))) {
                    return null;
                }
                return toUser(rs);
            }
        }
    }

    /** A-03 ユーザー一覧(ID 順) */
    public List<User> findAll() throws SQLException {
        String sql = SELECT_USER + "ORDER BY u.id\n";

        List<User> list = new ArrayList<>();
        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(toUser(rs));
            }
        }
        return list;
    }

    /** A-03 選択したユーザー(いなければ null) */
    public User findById(int id) throws SQLException {
        String sql = SELECT_USER + "WHERE u.id = ?\n";

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toUser(rs) : null;
            }
        }
    }

    /** ログイン ID がほかのユーザーで使われているか(exceptId のユーザーは除く。新規登録は 0) */
    public boolean existsLoginId(String loginId, int exceptId) throws SQLException {
        String sql =
                """
                SELECT
                    COUNT(*) AS cnt
                FROM
                    users u
                WHERE u.login_id = ?
                  AND u.id <> ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, loginId);
            ps.setInt(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt") > 0;
            }
        }
    }

    /**
     * A-03 登録:パスワードは BCrypt でハッシュ化して保存する(ソルトはハッシュ値に含まれる)。
     * 登録日時は DB の現在日時(SYSDATE)。登録したユーザーの ID を返す
     */
    public int insert(User user, String password) throws SQLException {
        String sql =
                """
                INSERT INTO users (id, login_id, password, name, role, status, created_datetime)
                VALUES (seq_users.NEXTVAL, ?, ?, ?, ?, ?, SYSDATE)
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, new String[] { "id" })) {
            ps.setString(1, user.getLoginId());
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, user.getName());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * A-03 変更:password が null のときはパスワードを変えない(登録日時は変えない)
     * 変更できたら true(ほかの端末で削除されていたら false)
     */
    public boolean update(User user, String password) throws SQLException {
        String sql = password == null
                ? """
                  UPDATE users
                  SET login_id = ?, name = ?, role = ?, status = ?
                  WHERE id = ?
                  """
                : """
                  UPDATE users
                  SET login_id = ?, name = ?, role = ?, status = ?, password = ?
                  WHERE id = ?
                  """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getLoginId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getRole());
            ps.setString(4, user.getStatus());
            if (password == null) {
                ps.setInt(5, user.getId());
            } else {
                ps.setString(5, PasswordUtil.hash(password));
                ps.setInt(6, user.getId());
            }
            return ps.executeUpdate() == 1;
        }
    }

    /** 取り消しの記録(ORDER_DETAILS.USER_ID)に使われている件数。使われているユーザーは削除できない */
    public int countCancelRecords(int id) throws SQLException {
        String sql =
                """
                SELECT
                    COUNT(*) AS cnt
                FROM
                    order_details od
                WHERE od.user_id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        }
    }

    /** A-03 削除:削除できたら true(ほかの端末ですでに削除されていたら false) */
    public boolean delete(int id) throws SQLException {
        String sql =
                """
                DELETE FROM users
                WHERE id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** 一覧・選択で使う SELECT(パスワードのハッシュ値は取らない) */
    private static final String SELECT_USER =
            """
            SELECT
                u.id       AS uuser_id,
                u.login_id AS ulogin_id,
                u.name     AS uname,
                u.role     AS urole,
                u.status   AS ustatus,
                u.created_datetime AS ucreated_datetime
            FROM
                users u
            """;

    private User toUser(ResultSet rs) throws SQLException {
        User u = new User(
                rs.getInt("uuser_id"),
                rs.getString("ulogin_id"),
                rs.getString("uname"),
                rs.getString("urole"),
                rs.getString("ustatus"));
        u.setCreatedDatetime(rs.getTimestamp("ucreated_datetime"));   // DATE 型は Timestamp で取る
        return u;
    }
}
