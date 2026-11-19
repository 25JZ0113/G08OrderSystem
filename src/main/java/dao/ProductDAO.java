package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.Product;
import db.DBManager;

/**
 * Step4 商品テーブルへのアクセスをまとめた DAO
 * JOIN で同名列(ID・NAME・DESCRIPTION)がかぶるため、SELECT 項目には必ず別名を付ける
 * SQL はテキストブロック(""")で書く。""" の後ろには何も書かない
 */
public class ProductDAO {

    /** 販売状態「販売中」 */
    public static final String ON_SALE = "販売中";

    /** トッピングのカテゴリ名(メニュー一覧には表示しない) */
    public static final String TOPPING_CATEGORY = "トッピング";

    /** 商品 + カテゴリ名を取り出す SELECT(この後ろに WHERE・ORDER BY を付ける) */
    private static final String SELECT_BASE =
            """
                    SELECT
                        p.id           AS pid,
                        p.category_id  AS pcategory_id,
                        p.name         AS pname,
                        p.size_name    AS psize_name,
                        p.price        AS pprice,
                        p.tax_rate     AS ptax_rate,
                        p.sales_status AS psales_status,
                        p.description  AS pdescription,
                        p.image_url    AS pimage_url,
                        c.name         AS cname
                    FROM
                        products p
                        JOIN categories c ON p.category_id = c.id
                    """;

    /** 全商品を取得する(管理画面用) */
    public List<Product> findAll() throws SQLException {
        String        sql  = SELECT_BASE
                + """
                        ORDER BY c.id, p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(toProduct(rs));
            }
        }
        return list;
    }

    /** ID を指定して商品を 1 件取得する(見つからなければ null) */
    public Product findById(int id) throws SQLException {
        String sql = SELECT_BASE
                + """
                        WHERE p.id = ?
                        """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * 商品名を指定して検索する(名前の一部が一致するもの)
     * 例: "ラテ" → カフェラテ、抹茶ラテ など
     */
    public List<Product> findByName(String name) throws SQLException {
        String        sql  = SELECT_BASE
                + """
                        WHERE p.name LIKE ?
                        ORDER BY c.id, p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toProduct(rs));
                }
            }
        }
        return list;
    }

    /** 販売状態を指定して商品一覧を取得する(例: "販売中") */
    public List<Product> findBySalesStatus(String salesStatus) throws SQLException {
        String        sql  = SELECT_BASE
                + """
                        WHERE p.sales_status = ?
                        ORDER BY c.id, p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, salesStatus);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toProduct(rs));
                }
            }
        }
        return list;
    }

    /**
     * お客様向けメニュー一覧を取得する
     * 販売中の商品のうち、トッピングのカテゴリを除いたもの
     */
    public List<Product> findMenu() throws SQLException {
        String        sql  = SELECT_BASE
                + """
                        WHERE p.sales_status = ?
                          AND c.name <> ?
                        ORDER BY c.id, p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ON_SALE);
            ps.setString(2, TOPPING_CATEGORY);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toProduct(rs));
                }
            }
        }
        return list;
    }

    /**
     * サイズ違いの商品を取得する(商品詳細画面のサイズ選択用)
     * 同じカテゴリ・同じ商品名で販売中のもの。ID 順(S → M → L)
     */
    public List<Product> findVariants(int categoryId, String name) throws SQLException {
        String        sql  = SELECT_BASE
                + """
                        WHERE p.category_id = ?
                          AND p.name = ?
                          AND p.sales_status = ?
                        ORDER BY p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setString(2, name);
            ps.setString(3, ON_SALE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toProduct(rs));
                }
            }
        }
        return list;
    }

    /**
     * 商品に付けられるトッピングを取得する(PRODUCT_TOPPINGS で設定したもの)
     * トッピング自身も PRODUCTS の 1 行なので、SELECT_BASE の p がトッピング商品になる
     */
    public List<Product> findToppings(int productId) throws SQLException {
        String        sql  = SELECT_BASE
                + """
                            JOIN product_toppings pt ON pt.topping_product_id = p.id
                        WHERE pt.product_id = ?
                          AND p.sales_status = ?
                        ORDER BY p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setString(2, ON_SALE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toProduct(rs));
                }
            }
        }
        return list;
    }

    // ============================================================
    // A-04 メニュー(商品)管理画面
    // ============================================================

    /** 販売状態「販売停止」 */
    public static final String STOPPED = "販売停止";

    /** カテゴリの商品一覧(販売停止も含めて全部。ID 順) */
    public List<Product> findByCategory(int categoryId) throws SQLException {
        String        sql  = SELECT_BASE
                + """
                        WHERE p.category_id = ?
                        ORDER BY p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toProduct(rs));
                }
            }
        }
        return list;
    }

    /** 同じカテゴリに同じ商品名・サイズの商品がほかにあるか(exceptId の商品は除く。登録は 0) */
    public boolean existsSame(int categoryId, String name, String sizeName, int exceptId) throws SQLException {
        // サイズなし(NULL)どうしも同じとみなすため NVL でそろえて比べる
        String sql =
                """
                SELECT
                    COUNT(*) AS cnt
                FROM
                    products p
                WHERE p.category_id = ?
                  AND p.name = ?
                  AND NVL(p.size_name, '-') = NVL(?, '-')
                  AND p.id <> ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setString(2, name);
            ps.setString(3, sizeName);
            ps.setInt(4, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt") > 0;
            }
        }
    }

    /** 商品を登録して、登録した商品の ID を返す */
    public int insert(Product p) throws SQLException {
        String sql =
                """
                INSERT INTO products
                    (id, category_id, name, size_name, price, tax_rate, sales_status, description, image_url)
                VALUES
                    (seq_products.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, new String[] { "id" })) {
            setProductParams(ps, p);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * 商品を変更する。変更できたら true(ほかの端末で削除されていたら false)
     * 価格を変えても、注文明細に保存した販売価格(SALE_PRICE)は変わらない
     */
    public boolean update(Product p) throws SQLException {
        String sql =
                """
                UPDATE products
                SET category_id = ?, name = ?, size_name = ?, price = ?, tax_rate = ?,
                    sales_status = ?, description = ?, image_url = ?
                WHERE id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            setProductParams(ps, p);
            ps.setInt(9, p.getId());
            return ps.executeUpdate() == 1;
        }
    }

    private void setProductParams(PreparedStatement ps, Product p) throws SQLException {
        ps.setInt(1, p.getCategoryId());
        ps.setString(2, p.getName());
        ps.setString(3, p.getSizeName());
        ps.setInt(4, p.getPrice());
        ps.setInt(5, p.getTaxRate());
        ps.setString(6, p.getSalesStatus());
        ps.setString(7, p.getDescription());
        ps.setString(8, p.getImageUrl());
    }

    /** 注文明細に使われている数(注文されたことがある商品は削除できない。販売停止にする) */
    public int countOrderDetails(int productId) throws SQLException {
        String sql =
                """
                SELECT
                    COUNT(*) AS cnt
                FROM
                    order_details od
                WHERE od.product_id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        }
    }

    /**
     * 商品を削除する。トッピングの設定(PRODUCT_TOPPINGS)も一緒に消す(1 つのトランザクション)
     * 削除できたら true(ほかの端末ですでに削除されていたら false)
     */
    public boolean delete(int productId) throws SQLException {
        String deleteToppings =
                """
                DELETE FROM product_toppings
                WHERE product_id = ?
                   OR topping_product_id = ?
                """;
        String deleteProduct =
                """
                DELETE FROM products
                WHERE id = ?
                """;

        try (Connection con = DBManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(deleteToppings);
                    PreparedStatement ps2 = con.prepareStatement(deleteProduct)) {
                ps1.setInt(1, productId);
                ps1.setInt(2, productId);
                ps1.executeUpdate();
                ps2.setInt(1, productId);
                boolean deleted = ps2.executeUpdate() == 1;
                con.commit();
                return deleted;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** 商品に付けているトッピング(販売停止も含む。管理画面用) */
    public List<Product> findAttachedToppings(int productId) throws SQLException {
        String        sql  = SELECT_BASE
                + """
                            JOIN product_toppings pt ON pt.topping_product_id = p.id
                        WHERE pt.product_id = ?
                        ORDER BY p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toProduct(rs));
                }
            }
        }
        return list;
    }

    /** まだ付けていないトッピング(トッピングのカテゴリの商品のうち、この商品に付けていないもの) */
    public List<Product> findToppingCandidates(int productId) throws SQLException {
        String        sql  = SELECT_BASE
                + """
                        WHERE c.name = ?
                          AND p.id NOT IN (
                              SELECT pt.topping_product_id
                              FROM product_toppings pt
                              WHERE pt.product_id = ?
                          )
                        ORDER BY p.id
                        """;
        List<Product> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, TOPPING_CATEGORY);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toProduct(rs));
                }
            }
        }
        return list;
    }

    /** トッピングを付ける(すでに付いていたら一意制約のエラーになる) */
    public void addTopping(int productId, int toppingProductId) throws SQLException {
        String sql =
                """
                INSERT INTO product_toppings (id, product_id, topping_product_id)
                VALUES (seq_product_toppings.NEXTVAL, ?, ?)
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, toppingProductId);
            ps.executeUpdate();
        }
    }

    /** 商品 fromId に付けられるトッピングを、商品 toId にも付ける(「この商品をもとに追加」で使う) */
    public void copyToppings(int fromId, int toId) throws SQLException {
        String sql =
                """
                INSERT INTO product_toppings (id, product_id, topping_product_id)
                SELECT seq_product_toppings.NEXTVAL, ?, pt.topping_product_id
                FROM product_toppings pt
                WHERE pt.product_id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, toId);
            ps.setInt(2, fromId);
            ps.executeUpdate();
        }
    }

    /** トッピングを外す。外せたら true */
    public boolean removeTopping(int productId, int toppingProductId) throws SQLException {
        String sql =
                """
                DELETE FROM product_toppings
                WHERE product_id = ?
                  AND topping_product_id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, toppingProductId);
            return ps.executeUpdate() == 1;
        }
    }

    /** ResultSet の 1 行を Product に詰め替える */
    private Product toProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("pid"),
                rs.getInt("pcategory_id"),
                rs.getString("cname"),
                rs.getString("pname"),
                rs.getString("psize_name"),
                rs.getInt("pprice"),
                rs.getInt("ptax_rate"),
                rs.getString("psales_status"),
                rs.getString("pdescription"),
                rs.getString("pimage_url"));
    }
}
