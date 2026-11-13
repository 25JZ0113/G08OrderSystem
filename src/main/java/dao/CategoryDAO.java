package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.Category;
import util.DBManager;

/**
 * カテゴリ(CATEGORIES)へのアクセス。A-04 メニュー(商品)管理画面で使う
 */
public class CategoryDAO {

    private static final String SELECT_CATEGORY =
            """
            SELECT
                c.id          AS cid,
                c.name        AS cname,
                c.description AS cdescription
            FROM
                categories c
            """;

    /** カテゴリ一覧(ID 順) */
    public List<Category> findAll() throws SQLException {
        String sql = SELECT_CATEGORY + "ORDER BY c.id\n";

        List<Category> list = new ArrayList<>();
        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(toCategory(rs));
            }
        }
        return list;
    }

    /** カテゴリ名がほかのカテゴリで使われているか(exceptId のカテゴリは除く。追加は 0) */
    public boolean existsName(String name, int exceptId) throws SQLException {
        String sql =
                """
                SELECT
                    COUNT(*) AS cnt
                FROM
                    categories c
                WHERE c.name = ?
                  AND c.id <> ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt") > 0;
            }
        }
    }

    /** 追加して、追加したカテゴリの ID を返す */
    public int insert(Category category) throws SQLException {
        String sql =
                """
                INSERT INTO categories (id, name, description)
                VALUES (seq_categories.NEXTVAL, ?, ?)
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, new String[] { "id" })) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** 変更できたら true(ほかの端末で削除されていたら false) */
    public boolean update(Category category) throws SQLException {
        String sql =
                """
                UPDATE categories
                SET name = ?, description = ?
                WHERE id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getId());
            return ps.executeUpdate() == 1;
        }
    }

    /** カテゴリに入っている商品の数(商品があるカテゴリは削除できない) */
    public int countProducts(int id) throws SQLException {
        String sql =
                """
                SELECT
                    COUNT(*) AS cnt
                FROM
                    products p
                WHERE p.category_id = ?
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

    /** 削除できたら true(ほかの端末ですでに削除されていたら false) */
    public boolean delete(int id) throws SQLException {
        String sql =
                """
                DELETE FROM categories
                WHERE id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Category toCategory(ResultSet rs) throws SQLException {
        return new Category(rs.getInt("cid"), rs.getString("cname"), rs.getString("cdescription"));
    }
}
