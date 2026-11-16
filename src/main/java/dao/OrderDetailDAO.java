package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.Cart;
import model.CartItem;
import model.Order;
import model.OrderDetail;
import model.OrderLine;
import model.Product;
import util.DBManager;

/**
 * 注文明細(ORDER_DETAILS)へのアクセス
 */
public class OrderDetailDAO {

    /** 注文の確定に失敗したときの理由(画面に出す) */
    public static class OrderConfirmException extends Exception {
        public OrderConfirmException(String message) {
            super(message);
        }
    }

    /**
     * カートの内容で注文を確定する(T-03「注文を確定する」)
     * 1. 注文が未会計か確認する(行ロックをかけて、同時の操作とぶつからないようにする)
     * 2. 商品・トッピングを 1 件ずつ注文明細に登録する
     * - 販売価格は今の DB の価格を保存する
     * - トッピングは数量を商品と同じにし、元注文明細 ID に商品の明細 ID を入れる
     * 3. 注文の合計金額を、明細から計算し直す
     * すべて 1 つのトランザクションで行い、途中で失敗したら何も登録しない
     *
     * @throws OrderConfirmException 販売停止・価格変更・会計済みなどで確定できないとき
     */
    public void confirm(int orderId, Cart cart) throws SQLException, OrderConfirmException {
        String lockOrder    =
                """
                        SELECT o.payment_status AS opayment_status
                          FROM orders o
                         WHERE o.id = ?
                           FOR UPDATE
                        """;
        String findProduct  =
                """
                        SELECT p.price AS pprice, p.sales_status AS psales_status
                          FROM products p
                         WHERE p.id = ?
                        """;
        String checkTopping =
                """
                        SELECT COUNT(*) AS cnt
                          FROM product_toppings pt
                         WHERE pt.product_id = ?
                           AND pt.topping_product_id = ?
                        """;
        String nextId       =
                """
                        SELECT seq_order_details.NEXTVAL AS next_id FROM dual
                        """;
        String insertDetail =
                """
                        INSERT INTO order_details
                            (id, order_id, product_id, original_order_detail_id, quantity, sale_price,
                             order_status, ordered_datetime, user_id, canceled_datetime)
                        VALUES
                            (?, ?, ?, ?, ?, ?, ?, SYSDATE, NULL, NULL)
                        """;
        // 合計金額:「キャンセル(請求なし)」以外の明細の 販売価格 × 数量 の合計
        String updateTotal =
                """
                        UPDATE orders
                           SET total_amount = (SELECT NVL(SUM(d.sale_price * d.quantity), 0)
                                                 FROM order_details d
                                                WHERE d.order_id = ?
                                                  AND d.order_status <> ?)
                         WHERE id = ?
                        """;

        try (Connection con = DBManager.getConnection()) {
            con.setAutoCommit(false);   // ここからトランザクション
            try (PreparedStatement psLock = con.prepareStatement(lockOrder);
                    PreparedStatement psProduct = con.prepareStatement(findProduct);
                    PreparedStatement psTopping = con.prepareStatement(checkTopping);
                    PreparedStatement psNextId = con.prepareStatement(nextId);
                    PreparedStatement psInsert = con.prepareStatement(insertDetail);
                    PreparedStatement psTotal = con.prepareStatement(updateTotal)) {

                // 1. 注文が未会計か(会計後の端末から注文されないように)
                psLock.setInt(1, orderId);
                try (ResultSet rs = psLock.executeQuery()) {
                    if (!rs.next() || !Order.UNPAID.equals(rs.getString("opayment_status"))) {
                        throw new OrderConfirmException("このテーブルの注文は受け付けできません。スタッフをお呼びください。");
                    }
                }

                // 2. 明細を登録する
                for (CartItem item : cart.getItems()) {
                    Product product = item.getProduct();
                    checkProduct(psProduct, product);
                    int detailId = insert(psNextId, psInsert, orderId, product.getId(), null,
                            item.getQuantity(), product.getPrice());

                    for (Product topping : item.getToppings()) {
                        checkProduct(psProduct, topping);
                        psTopping.setInt(1, product.getId());
                        psTopping.setInt(2, topping.getId());
                        try (ResultSet rs = psTopping.executeQuery()) {
                            rs.next();
                            if (rs.getInt("cnt") == 0) {
                                throw new OrderConfirmException(
                                        "「" + product.getName() + "」に付けられなくなったトッピングがあります。"
                                                + "カートの商品名を選んで、選び直してください。");
                            }
                        }
                        insert(psNextId, psInsert, orderId, topping.getId(), detailId,
                                item.getQuantity(), topping.getPrice());
                    }
                }

                // 3. 合計金額を更新する
                psTotal.setInt(1, orderId);
                psTotal.setString(2, OrderDetail.CANCELED_NO_CHARGE);
                psTotal.setInt(3, orderId);
                psTotal.executeUpdate();

                con.commit();
            } catch (SQLException | OrderConfirmException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * R-02 未提供の商品を取り消す(商品と、それに付いたトッピングをまとめて取り消す)
     * 注文受付済み              → キャンセル(請求なし)。合計金額に含めない
     * 調理中・調理済み(提供待ち) → キャンセル(請求あり)。合計金額に含める
     * 取り消した社員と日時を記録し、注文の合計金額を計算し直す
     * 画面を表示したときの状態(expectedStatus)から変わっていたら取り消さない
     * (例:その間に提供済みになっていた)
     *
     * @return 変更後の注文状態。状態が変わっていて取り消せなかったときは null
     */
    public String cancel(int orderId, int detailId, String expectedStatus, int userId) throws SQLException {
        String to = OrderDetail.canceledStatusOf(expectedStatus);
        if (to == null) {
            return null;   // 提供済み・取り消し済みは取り消せない
        }

        String updateDetail =
                """
                        UPDATE order_details
                           SET order_status      = ?,
                               user_id           = ?,
                               canceled_datetime = SYSDATE
                         WHERE order_id = ?
                           AND (id = ? OR original_order_detail_id = ?)
                           AND order_status = ?
                        """;
        String updateTotal =
                """
                        UPDATE orders
                           SET total_amount = (SELECT NVL(SUM(d.sale_price * d.quantity), 0)
                                                 FROM order_details d
                                                WHERE d.order_id = ?
                                                  AND d.order_status <> ?)
                         WHERE id = ?
                           AND payment_status = ?
                        """;

        try (Connection con = DBManager.getConnection()) {
            con.setAutoCommit(false);   // ここからトランザクション
            try (PreparedStatement psDetail = con.prepareStatement(updateDetail);
                    PreparedStatement psTotal = con.prepareStatement(updateTotal)) {
                psDetail.setString(1, to);
                psDetail.setInt(2, userId);
                psDetail.setInt(3, orderId);
                psDetail.setInt(4, detailId);
                psDetail.setInt(5, detailId);
                psDetail.setString(6, expectedStatus);
                if (psDetail.executeUpdate() == 0) {
                    con.rollback();
                    return null;
                }

                psTotal.setInt(1, orderId);
                psTotal.setString(2, OrderDetail.CANCELED_NO_CHARGE);
                psTotal.setInt(3, orderId);
                psTotal.setString(4, Order.UNPAID);
                if (psTotal.executeUpdate() == 0) {
                    // 会計済みの注文は取り消さない
                    con.rollback();
                    return null;
                }

                con.commit();
                return to;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * 注文の明細を、トッピングを元の商品にまとめた形で取得する(T-04 注文状況など)
     * 並び順は注文した順(明細 ID 順)
     */
    public List<OrderLine> findLinesByOrderId(int orderId) throws SQLException {
        String sql =
                """
                        SELECT
                            d.id                       AS did,
                            d.order_id                 AS dorder_id,
                            d.product_id               AS dproduct_id,
                            d.original_order_detail_id AS doriginal_id,
                            d.quantity                 AS dquantity,
                            d.sale_price               AS dsale_price,
                            d.order_status             AS dorder_status,
                            d.ordered_datetime         AS dordered_datetime,
                            d.canceled_datetime        AS dcanceled_datetime,
                            p.name                     AS pname,
                            p.size_name                AS psize_name,
                            p.tax_rate                 AS ptax_rate
                        FROM
                            order_details d
                            JOIN products p ON d.product_id = p.id
                        WHERE d.order_id = ?
                        ORDER BY d.id
                        """;

        // 明細 ID → 行(トッピングを親の行に入れるために使う)。LinkedHashMap なので注文順のまま
        Map<Integer, OrderLine> lines = new LinkedHashMap<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail d = new OrderDetail();
                    d.setId(rs.getInt("did"));
                    d.setOrderId(rs.getInt("dorder_id"));
                    d.setProductId(rs.getInt("dproduct_id"));
                    int originalId = rs.getInt("doriginal_id");
                    d.setOriginalOrderDetailId(rs.wasNull() ? null : originalId);   // NULL は wasNull で判定
                    d.setQuantity(rs.getInt("dquantity"));
                    d.setSalePrice(rs.getInt("dsale_price"));
                    d.setOrderStatus(rs.getString("dorder_status"));
                    d.setOrderedDatetime(rs.getTimestamp("dordered_datetime"));
                    d.setCanceledDatetime(rs.getTimestamp("dcanceled_datetime"));
                    d.setProductName(rs.getString("pname"));
                    d.setSizeName(rs.getString("psize_name"));
                    d.setTaxRate(rs.getInt("ptax_rate"));

                    OrderLine parent = (d.getOriginalOrderDetailId() == null)
                            ? null
                            : lines.get(d.getOriginalOrderDetailId());
                    if (parent != null) {
                        parent.addTopping(d);          // トッピング → 元の商品の行へ
                    }
                    else {
                        lines.put(d.getId(), new OrderLine(d));
                    }
                }
            }
        }
        return new ArrayList<>(lines.values());
    }

    /**
     * K-01 調理待ち一覧:「注文受付済み」と「調理中」の明細を、注文時刻の古い順に取得する
     */
    public List<OrderLine> findKitchenLines() throws SQLException {
        return findLinesByStatus(OrderDetail.RECEIVED, OrderDetail.COOKING);
    }

    /**
     * H-03 提供待ち一覧:「調理済み(提供待ち)」の明細を、調理完了時刻の古い順に取得する
     */
    public List<OrderLine> findServingLines() throws SQLException {
        return findLinesByStatus(OrderDetail.COOKED, OrderDetail.COOKED);
    }

    /**
     * 指定した 2 つの注文状態の明細を取得する(1 つだけなら同じ値を 2 回渡す)
     * 並び順:調理完了時刻の古い順(調理前で調理完了時刻がないものは注文時刻の古い順)
     * トッピングは元の商品の行にまとめる。テーブル番号も一緒に取る
     */
    private List<OrderLine> findLinesByStatus(String status1, String status2) throws SQLException {
        String sql =
                """
                SELECT
                    d.id                       AS did,
                    d.order_id                 AS dorder_id,
                    d.product_id               AS dproduct_id,
                    d.original_order_detail_id AS doriginal_id,
                    d.quantity                 AS dquantity,
                    d.sale_price               AS dsale_price,
                    d.order_status             AS dorder_status,
                    d.ordered_datetime         AS dordered_datetime,
                    d.cooked_datetime          AS dcooked_datetime,
                    d.canceled_datetime        AS dcanceled_datetime,
                    p.name                     AS pname,
                    p.size_name                AS psize_name,
                    t.table_no                 AS ttable_no
                FROM
                    order_details d
                    JOIN products p ON d.product_id = p.id
                    JOIN orders o   ON d.order_id = o.id
                    JOIN tables t   ON o.table_id = t.id
                WHERE d.order_status IN (?, ?)
                ORDER BY NVL(d.cooked_datetime, d.ordered_datetime), d.ordered_datetime, d.id
                """;

        Map<Integer, OrderLine> lines = new LinkedHashMap<>();   // 商品の明細(注文時刻の古い順)
        List<OrderDetail> toppings = new ArrayList<>();         // トッピングは後で元の商品に付ける

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status1);
            ps.setString(2, status2);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail d = new OrderDetail();
                    d.setId(rs.getInt("did"));
                    d.setOrderId(rs.getInt("dorder_id"));
                    d.setProductId(rs.getInt("dproduct_id"));
                    int originalId = rs.getInt("doriginal_id");
                    d.setOriginalOrderDetailId(rs.wasNull() ? null : originalId);   // NULL は wasNull で判定
                    d.setQuantity(rs.getInt("dquantity"));
                    d.setSalePrice(rs.getInt("dsale_price"));
                    d.setOrderStatus(rs.getString("dorder_status"));
                    d.setOrderedDatetime(rs.getTimestamp("dordered_datetime"));    // DATE 型は Timestamp で取る
                    d.setCookedDatetime(rs.getTimestamp("dcooked_datetime"));      // 調理前は null
                    d.setCanceledDatetime(rs.getTimestamp("dcanceled_datetime"));
                    d.setProductName(rs.getString("pname"));
                    d.setSizeName(rs.getString("psize_name"));
                    d.setTableNo(rs.getInt("ttable_no"));

                    if (d.getOriginalOrderDetailId() == null) {
                        lines.put(d.getId(), new OrderLine(d));
                    }
                    else {
                        toppings.add(d);
                    }
                }
            }
        }

        // トッピング → 元の商品の行へ(元の商品が一覧にないトッピングは表示しない)
        for (OrderDetail t : toppings) {
            OrderLine parent = lines.get(t.getOriginalOrderDetailId());
            if (parent != null) {
                parent.addTopping(t);
            }
        }
        return new ArrayList<>(lines.values());
    }

    /**
     * 注文状態を変更する(K-01 調理中にする・調理済みにする、H-03 提供済みにする)
     * 商品の明細と、それに付いたトッピングの明細を一緒に変更する
     * 今の状態が from のときだけ変更する(2 回押された・別の端末で変更済みのときは変更しない)
     * 「調理済み(提供待ち)」にするときは、調理完了日時(COOKED_DATETIME)に今の日時を入れる
     *
     * @return 変更できたら true
     */
    public boolean changeStatus(int detailId, String from, String to) throws SQLException {
        String sql;
        if (OrderDetail.COOKED.equals(to)) {
            sql =
                    """
                    UPDATE order_details
                       SET order_status = ?,
                           cooked_datetime = SYSDATE
                     WHERE (id = ? OR original_order_detail_id = ?)
                       AND order_status = ?
                    """;
        }
        else {
            sql =
                    """
                    UPDATE order_details
                       SET order_status = ?
                     WHERE (id = ? OR original_order_detail_id = ?)
                       AND order_status = ?
                    """;
        }

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, to);
            ps.setInt(2, detailId);
            ps.setInt(3, detailId);
            ps.setString(4, from);
            return ps.executeUpdate() > 0;
        }
    }

    /** 商品が今も販売中で、カートに入れたときと価格が同じか確認する */
    private void checkProduct(PreparedStatement ps, Product product)
            throws SQLException, OrderConfirmException {
        ps.setInt(1, product.getId());
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next() || !ProductDAO.ON_SALE.equals(rs.getString("psales_status"))) {
                throw new OrderConfirmException(
                        "「" + product.getName() + "」は販売を終了しました。カートから削除してください。");
            }
            if (rs.getInt("pprice") != product.getPrice()) {
                throw new OrderConfirmException("「" + product.getName() + "」の価格が変わりました。"
                        + "カートから削除して、もう一度選んでください。");
            }
        }
    }

    /** 明細を 1 件登録して、採番した明細 ID を返す */
    private int insert(PreparedStatement psNextId, PreparedStatement psInsert, int orderId,
            int productId,
            Integer originalId, int quantity, int salePrice) throws SQLException {
        int id;
        try (ResultSet rs = psNextId.executeQuery()) {
            rs.next();
            id = rs.getInt("next_id");
        }
        psInsert.setInt(1, id);
        psInsert.setInt(2, orderId);
        psInsert.setInt(3, productId);
        if (originalId == null) {
            psInsert.setNull(4, java.sql.Types.NUMERIC);
        }
        else {
            psInsert.setInt(4, originalId);
        }
        psInsert.setInt(5, quantity);
        psInsert.setInt(6, salePrice);
        psInsert.setString(7, OrderDetail.RECEIVED);
        psInsert.executeUpdate();
        return id;
    }
}
