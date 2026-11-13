package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import model.Order;
import model.OrderDetail;
import model.TableTerminal;
import util.DBManager;

/**
 * 注文(ORDERS)へのアクセス
 */
public class OrderDAO {

    /**
     * テーブルの未会計の注文(今の来店)を取得する(なければ null)
     * 念のため、未会計が複数あるときは来店日時が新しいものを返す
     */
    public Order findUnpaidByTableId(int tableId) throws SQLException {
        String sql =
                """
                        SELECT
                            o.id               AS oid,
                            o.table_id         AS otable_id,
                            o.guest_count      AS oguest_count,
                            o.visit_datetime   AS ovisit_datetime,
                            o.payment_datetime AS opayment_datetime,
                            o.payment_status   AS opayment_status,
                            o.payment_method   AS opayment_method,
                            o.total_amount     AS ototal_amount
                        FROM
                            orders o
                        WHERE o.table_id = ?
                          AND o.payment_status = ?
                        ORDER BY o.visit_datetime DESC
                        """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setString(2, Order.UNPAID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order o = new Order();
                    o.setId(rs.getInt("oid"));
                    o.setTableId(rs.getInt("otable_id"));
                    o.setGuestCount(rs.getInt("oguest_count"));
                    o.setVisitDatetime(rs.getTimestamp("ovisit_datetime"));    // DATE 型は Timestamp で取る
                    o.setPaymentDatetime(rs.getTimestamp("opayment_datetime"));
                    o.setPaymentStatus(rs.getString("opayment_status"));
                    o.setPaymentMethod(rs.getString("opayment_method"));
                    o.setTotalAmount(rs.getInt("ototal_amount"));
                    return o;
                }
            }
        }
        return null;
    }

    /** R-01〜R-03 レジ:未会計の注文 + テーブル番号の SELECT */
    private static final String SELECT_UNPAID_WITH_TABLE =
            """
            SELECT
                o.id             AS oid,
                o.table_id       AS otable_id,
                o.guest_count    AS oguest_count,
                o.visit_datetime AS ovisit_datetime,
                o.payment_status AS opayment_status,
                o.total_amount   AS ototal_amount,
                t.table_no       AS ttable_no
            FROM
                orders o
                JOIN tables t ON o.table_id = t.id
            """;

    /**
     * R-01 テーブル番号入力:テーブル番号から未会計の注文を取得する(なければ null)
     */
    public Order findUnpaidByTableNo(int tableNo) throws SQLException {
        String sql = SELECT_UNPAID_WITH_TABLE + """
                WHERE t.table_no = ?
                  AND o.payment_status = ?
                ORDER BY o.visit_datetime DESC
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tableNo);
            ps.setString(2, Order.UNPAID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toUnpaidOrder(rs) : null;
            }
        }
    }

    /**
     * R-02・R-03:注文 ID から未会計の注文を取得する(会計済み・見つからないときは null)
     */
    public Order findUnpaidById(int orderId) throws SQLException {
        String sql = SELECT_UNPAID_WITH_TABLE + """
                WHERE o.id = ?
                  AND o.payment_status = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, Order.UNPAID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toUnpaidOrder(rs) : null;
            }
        }
    }

    private Order toUnpaidOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("oid"));
        o.setTableId(rs.getInt("otable_id"));
        o.setGuestCount(rs.getInt("oguest_count"));
        o.setVisitDatetime(rs.getTimestamp("ovisit_datetime"));   // DATE 型は Timestamp で取る
        o.setPaymentStatus(rs.getString("opayment_status"));
        o.setTotalAmount(rs.getInt("ototal_amount"));
        o.setTableNo(rs.getInt("ttable_no"));
        return o;
    }

    /** 会計を確定できなかった理由(画面に出す) */
    public static class PaymentException extends Exception {
        public PaymentException(String message) {
            super(message);
        }
    }

    /**
     * R-03 会計を確定する
     * 1. 注文が未会計か確認する(行ロックをかけて、二重に会計されないようにする)
     * 2. 未提供の明細が残っていないか確認する
     * 3. 合計金額を明細から計算し直し、会計状態を「会計済み」、会計日時・支払方法を記録する
     * 4. テーブル状態を「準備中」にする(テーブル端末は待機画面になる)
     * すべて 1 つのトランザクションで行う
     *
     * @param received 預かり金額(現金のときだけ。それ以外は null)
     * @return 確定した合計金額
     * @throws PaymentException 会計済み・未提供あり・預かり金額不足で確定できないとき
     */
    public int pay(int orderId, String paymentMethod, Integer received)
            throws SQLException, PaymentException {
        String lockOrder   =
                """
                        SELECT o.payment_status AS opayment_status,
                               o.table_id       AS otable_id
                          FROM orders o
                         WHERE o.id = ?
                           FOR UPDATE
                        """;
        String countUnserved =
                """
                        SELECT COUNT(*) AS cnt
                          FROM order_details d
                         WHERE d.order_id = ?
                           AND d.order_status IN (?, ?, ?)
                        """;
        // 合計金額:「キャンセル(請求なし)」以外の明細の 販売価格 × 数量 の合計
        String sumTotal =
                """
                        SELECT NVL(SUM(d.sale_price * d.quantity), 0) AS total
                          FROM order_details d
                         WHERE d.order_id = ?
                           AND d.order_status <> ?
                        """;
        String updateOrder =
                """
                        UPDATE orders
                           SET payment_status   = ?,
                               payment_datetime = SYSDATE,
                               payment_method   = ?,
                               total_amount     = ?
                         WHERE id = ?
                        """;
        String updateTable =
                """
                        UPDATE tables
                           SET status = ?
                         WHERE id = ?
                        """;

        try (Connection con = DBManager.getConnection()) {
            con.setAutoCommit(false);   // ここからトランザクション
            try {
                // 1. 未会計か
                int tableId;
                try (PreparedStatement ps = con.prepareStatement(lockOrder)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !Order.UNPAID.equals(rs.getString("opayment_status"))) {
                            throw new PaymentException("この注文はすでに会計済みです。");
                        }
                        tableId = rs.getInt("otable_id");
                    }
                }

                // 2. 未提供が残っていないか
                try (PreparedStatement ps = con.prepareStatement(countUnserved)) {
                    ps.setInt(1, orderId);
                    ps.setString(2, OrderDetail.RECEIVED);
                    ps.setString(3, OrderDetail.COOKING);
                    ps.setString(4, OrderDetail.COOKED);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt("cnt") > 0) {
                            throw new PaymentException("未提供の商品があります。提供するか取り消してから会計してください。");
                        }
                    }
                }

                // 3. 合計金額を計算し直して会計済みにする
                int total;
                try (PreparedStatement ps = con.prepareStatement(sumTotal)) {
                    ps.setInt(1, orderId);
                    ps.setString(2, OrderDetail.CANCELED_NO_CHARGE);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        total = rs.getInt("total");
                    }
                }
                // 現金:預かり金額が合計金額より少ないときは確定しない
                if (received != null && received < total) {
                    throw new PaymentException("預かり金額が合計金額より少ないため、会計を確定できません。");
                }
                try (PreparedStatement ps = con.prepareStatement(updateOrder)) {
                    ps.setString(1, Order.PAID);
                    ps.setString(2, paymentMethod);
                    ps.setInt(3, total);
                    ps.setInt(4, orderId);
                    ps.executeUpdate();
                }

                // 4. テーブルを準備中にする
                try (PreparedStatement ps = con.prepareStatement(updateTable)) {
                    ps.setString(1, TableTerminal.STATUS_PREPARING);
                    ps.setInt(2, tableId);
                    ps.executeUpdate();
                }

                con.commit();
                return total;
            } catch (SQLException | PaymentException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * 未会計の注文の人数を取得する(会計済み・見つからないときは null)
     * テーブル端末のヘッダーの人数を、ホールでの人数変更(H-02)に合わせるために使う
     */
    public Integer findUnpaidGuestCount(int orderId) throws SQLException {
        String sql =
                """
                        SELECT
                            o.guest_count AS oguest_count
                        FROM
                            orders o
                        WHERE o.id = ?
                          AND o.payment_status = ?
                        """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, Order.UNPAID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("oguest_count") : null;
            }
        }
    }

    /**
     * H-02 人数変更:今の来店(未会計の注文)の人数だけを変更する
     * テーブル状態は変えない。会計が済んでいたら(別の端末で変わっていたら)変更しない
     *
     * @return 変更できたら true
     */
    public boolean changeGuestCount(int orderId, int guestCount) throws SQLException {
        String sql =
                """
                        UPDATE orders
                           SET guest_count = ?
                         WHERE id = ?
                           AND payment_status = ?
                        """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, guestCount);
            ps.setInt(2, orderId);
            ps.setString(3, Order.UNPAID);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 来店を開始する(T-08 で人数を入力したとき)
     * 1. テーブル状態を「空席」→「利用中」にする
     * 2. 注文(未会計・合計 0 円)を登録する
     * 2 つを 1 つのトランザクションで行う。テーブルが空席でなければ何もせず null を返す
     *
     * @return 登録した注文の ID(空席でなかったときは null)
     */
    public Integer startVisit(int tableId, int guestCount) throws SQLException {
        String updateTable =
                """
                        UPDATE tables
                           SET status = ?
                         WHERE id = ?
                           AND status = ?
                        """;
        String nextId      =
                """
                        SELECT seq_orders.NEXTVAL AS next_id FROM dual
                        """;
        String insertOrder =
                """
                        INSERT INTO orders
                            (id, table_id, guest_count, visit_datetime, payment_datetime,
                             payment_status, payment_method, total_amount)
                        VALUES
                            (?, ?, ?, SYSDATE, NULL, ?, NULL, 0)
                        """;

        try (Connection con = DBManager.getConnection()) {
            con.setAutoCommit(false);   // ここからトランザクション
            try {
                // 1. 空席のときだけ利用中にする(同時に押されても 1 回だけ成功する)
                int updated;
                try (PreparedStatement ps = con.prepareStatement(updateTable)) {
                    ps.setString(1, TableTerminal.STATUS_IN_USE);
                    ps.setInt(2, tableId);
                    ps.setString(3, TableTerminal.STATUS_VACANT);
                    updated = ps.executeUpdate();
                }
                if (updated != 1) {
                    con.rollback();
                    return null;
                }

                // 2. シーケンスで注文 ID を採番して登録する
                int orderId;
                try (PreparedStatement ps = con.prepareStatement(nextId);
                        ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    orderId = rs.getInt("next_id");
                }
                try (PreparedStatement ps = con.prepareStatement(insertOrder)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, tableId);
                    ps.setInt(3, guestCount);
                    ps.setString(4, Order.UNPAID);
                    ps.executeUpdate();
                }

                con.commit();
                return orderId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
