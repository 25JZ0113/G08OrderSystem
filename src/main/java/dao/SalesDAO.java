package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.Order;
import model.OrderDetail;
import model.SalesRow;
import db.DBManager;

/**
 * A-05 売上管理画面の集計
 * 売上は「会計済み」の注文だけを、会計日時(PAYMENT_DATETIME)の日付で集計する
 *   総売上 … 注文明細の販売価格 × 数量の合計。キャンセル(請求なし)の商品は売上に入れない
 *            (キャンセル(請求あり)の商品は会計で請求するので売上に入れる)
 *   総客数 … 人数(GUEST_COUNT)の合計  総来店数 … 会計した注文の数
 * 期間は from 〜 to の両日を含む(SQL では payment_datetime >= from AND payment_datetime < to の翌日)
 */
public class SalesDAO {

    private static final String PAID = "会計済み";

    /** 売上状況:{来店数, 客数, 売上} を 1 行で返す */
    public SalesRow summary(LocalDate from, LocalDate to) throws SQLException {
        String sql =
                """
                SELECT
                    COUNT(*)                    AS visits,
                    NVL(SUM(o.guest_count), 0)  AS guests,
                    NVL(SUM(d.damount), 0)      AS amount
                FROM
                    orders o
                    LEFT JOIN (
                        SELECT
                            od.order_id                       AS dorder_id,
                            SUM(od.sale_price * od.quantity)  AS damount
                        FROM
                            order_details od
                        WHERE od.order_status <> ?
                        GROUP BY od.order_id
                    ) d ON d.dorder_id = o.id
                WHERE o.payment_status = ?
                  AND o.payment_datetime >= ?
                  AND o.payment_datetime <  ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new SalesRow("合計", rs.getInt("visits"), rs.getInt("guests"), 0, rs.getLong("amount"));
            }
        }
    }

    /** 日別:期間のすべての日を返す(売上のない日は 0) */
    public List<SalesRow> byDay(LocalDate from, LocalDate to) throws SQLException {
        Map<LocalDate, SalesRow> rows = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            SalesRow row = new SalesRow(String.format("%02d/%02d", d.getMonthValue(), d.getDayOfMonth()));
            row.setKey(d.toString());
            rows.put(d, row);
        }
        String sql =
                """
                SELECT
                    TRUNC(o.payment_datetime)   AS pay_day,
                    COUNT(*)                    AS visits,
                    SUM(o.guest_count)          AS guests,
                    NVL(SUM(d.damount), 0)      AS amount
                FROM
                    orders o
                    LEFT JOIN (
                        SELECT
                            od.order_id                       AS dorder_id,
                            SUM(od.sale_price * od.quantity)  AS damount
                        FROM
                            order_details od
                        WHERE od.order_status <> ?
                        GROUP BY od.order_id
                    ) d ON d.dorder_id = o.id
                WHERE o.payment_status = ?
                  AND o.payment_datetime >= ?
                  AND o.payment_datetime <  ?
                GROUP BY TRUNC(o.payment_datetime)
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate day = rs.getTimestamp("pay_day").toLocalDateTime().toLocalDate();
                    rows.get(day).add(rs.getInt("visits"), rs.getInt("guests"), rs.getLong("amount"));
                }
            }
        }
        return new ArrayList<>(rows.values());
    }

    /** 曜日別:月〜日の 7 行を返す */
    public List<SalesRow> byWeekday(LocalDate from, LocalDate to) throws SQLException {
        String[] names = { "月", "火", "水", "木", "金", "土", "日" };
        List<SalesRow> rows = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            SalesRow row = new SalesRow(names[i]);
            row.setKey(String.valueOf(i));   // 月=0 〜 日=6
            rows.add(row);
        }
        // TRUNC(日付, 'IW') は その週の月曜日。差を取ると 月=0 〜 日=6 になる(言語の設定によらない)
        String sql =
                """
                SELECT
                    TRUNC(o.payment_datetime) - TRUNC(o.payment_datetime, 'IW') AS wd,
                    COUNT(*)                    AS visits,
                    SUM(o.guest_count)          AS guests,
                    NVL(SUM(d.damount), 0)      AS amount
                FROM
                    orders o
                    LEFT JOIN (
                        SELECT
                            od.order_id                       AS dorder_id,
                            SUM(od.sale_price * od.quantity)  AS damount
                        FROM
                            order_details od
                        WHERE od.order_status <> ?
                        GROUP BY od.order_id
                    ) d ON d.dorder_id = o.id
                WHERE o.payment_status = ?
                  AND o.payment_datetime >= ?
                  AND o.payment_datetime <  ?
                GROUP BY TRUNC(o.payment_datetime) - TRUNC(o.payment_datetime, 'IW')
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.get(rs.getInt("wd")).add(rs.getInt("visits"), rs.getInt("guests"), rs.getLong("amount"));
                }
            }
        }
        return rows;
    }

    /** 月別:期間のすべての月を返す(売上のない月は 0) */
    public List<SalesRow> byMonth(LocalDate from, LocalDate to) throws SQLException {
        Map<LocalDate, SalesRow> rows = new LinkedHashMap<>();
        for (LocalDate m = from.withDayOfMonth(1); !m.isAfter(to); m = m.plusMonths(1)) {
            SalesRow row = new SalesRow(String.format("%d/%02d", m.getYear(), m.getMonthValue()));
            row.setKey(String.format("%d-%02d", m.getYear(), m.getMonthValue()));
            rows.put(m, row);
        }
        String sql =
                """
                SELECT
                    TRUNC(o.payment_datetime, 'MM') AS pay_month,
                    COUNT(*)                    AS visits,
                    SUM(o.guest_count)          AS guests,
                    NVL(SUM(d.damount), 0)      AS amount
                FROM
                    orders o
                    LEFT JOIN (
                        SELECT
                            od.order_id                       AS dorder_id,
                            SUM(od.sale_price * od.quantity)  AS damount
                        FROM
                            order_details od
                        WHERE od.order_status <> ?
                        GROUP BY od.order_id
                    ) d ON d.dorder_id = o.id
                WHERE o.payment_status = ?
                  AND o.payment_datetime >= ?
                  AND o.payment_datetime <  ?
                GROUP BY TRUNC(o.payment_datetime, 'MM')
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate month = rs.getTimestamp("pay_month").toLocalDateTime().toLocalDate();
                    rows.get(month).add(rs.getInt("visits"), rs.getInt("guests"), rs.getLong("amount"));
                }
            }
        }
        return new ArrayList<>(rows.values());
    }

    /**
     * 商品別:売上金額の多い順(同じ金額はカテゴリ順・商品 ID 順)
     * 期間中に売れていない商品も、数量 0・売上 0 円で出す(商品テーブルの全商品)
     * 取り消した商品は入れない。トッピングも 1 つの商品として数える
     */
    public List<SalesRow> byProduct(LocalDate from, LocalDate to) throws SQLException {
        String sql =
                """
                SELECT
                    p.id                   AS pid,
                    p.name                 AS pname,
                    p.size_name            AS psize_name,
                    NVL(s.sqty, 0)         AS qty,
                    NVL(s.samount, 0)      AS amount
                FROM
                    products p
                    LEFT JOIN (
                        -- 期間中に売れた数量と金額(商品ごと)
                        SELECT
                            od.product_id                     AS sproduct_id,
                            SUM(od.quantity)                  AS sqty,
                            SUM(od.sale_price * od.quantity)  AS samount
                        FROM
                            orders o
                            JOIN order_details od ON od.order_id = o.id
                        WHERE od.order_status <> ?
                          AND o.payment_status = ?
                          AND o.payment_datetime >= ?
                          AND o.payment_datetime <  ?
                        GROUP BY od.product_id
                    ) s ON s.sproduct_id = p.id
                ORDER BY amount DESC, p.category_id, p.id
                """;

        List<SalesRow> rows = new ArrayList<>();
        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String size  = rs.getString("psize_name");
                    String label = rs.getString("pname") + (size == null ? "" : "(" + size + ")");
                    SalesRow row = new SalesRow(label, 0, 0, rs.getInt("qty"), rs.getLong("amount"));
                    row.setKey(String.valueOf(rs.getInt("pid")));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    /**
     * A-06 売上の注文一覧:期間中に会計した注文を、会計した日時の順に返す(多すぎるときは max 件まで)
     *
     * @param weekday   曜日で絞るとき 0(月)〜6(日)。絞らないときは null
     * @param productId この商品を含む注文に絞るとき商品 ID(キャンセル(請求なし)の明細は数えない)。絞らないときは null
     * @param max       返す件数の上限
     */
    public List<Order> findOrders(LocalDate from, LocalDate to, Integer weekday, Integer productId, int max)
            throws SQLException {
        String sql =
                """
                SELECT
                    o.id                AS oid,
                    o.table_id          AS otable_id,
                    o.guest_count       AS oguest_count,
                    o.visit_datetime    AS ovisit_datetime,
                    o.payment_datetime  AS opayment_datetime,
                    o.payment_status    AS opayment_status,
                    o.payment_method    AS opayment_method,
                    o.total_amount      AS ototal_amount,
                    t.table_no          AS ttable_no
                FROM
                    orders o
                    JOIN tables t ON o.table_id = t.id
                WHERE o.payment_status = ?
                  AND o.payment_datetime >= ?
                  AND o.payment_datetime <  ?
                """;
        if (weekday != null) {
            // 売上の曜日別と同じ求め方(月=0 〜 日=6)
            sql += """
                  AND TRUNC(o.payment_datetime) - TRUNC(o.payment_datetime, 'IW') = ?
                """;
        }
        if (productId != null) {
            sql += """
                  AND EXISTS (
                      SELECT 1
                      FROM order_details od
                      WHERE od.order_id = o.id
                        AND od.product_id = ?
                        AND od.order_status <> ?
                  )
                """;
        }
        sql += """
                ORDER BY o.payment_datetime, o.id
                FETCH FIRST ? ROWS ONLY
                """;

        List<Order> orders = new ArrayList<>();
        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, PAID);
            ps.setTimestamp(i++, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(i++, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            if (weekday != null) {
                ps.setInt(i++, weekday);
            }
            if (productId != null) {
                ps.setInt(i++, productId);
                ps.setString(i++, OrderDetail.CANCELED_NO_CHARGE);
            }
            ps.setInt(i, max);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = new Order();
                    o.setId(rs.getInt("oid"));
                    o.setTableId(rs.getInt("otable_id"));
                    o.setTableNo(rs.getInt("ttable_no"));
                    o.setGuestCount(rs.getInt("oguest_count"));
                    o.setVisitDatetime(rs.getTimestamp("ovisit_datetime"));
                    o.setPaymentDatetime(rs.getTimestamp("opayment_datetime"));
                    o.setPaymentStatus(rs.getString("opayment_status"));
                    o.setPaymentMethod(rs.getString("opayment_method"));
                    o.setTotalAmount(rs.getInt("ototal_amount"));
                    orders.add(o);
                }
            }
        }
        return orders;
    }

    /**
     * どの SQL も ? の順番は同じ:1 番目 … 売上に入れない取り消し(キャンセル(請求なし))
     *                          2 番目 … 「会計済み」  3・4 番目 … 期間
     */
    private void setPeriod(PreparedStatement ps, LocalDate from, LocalDate to) throws SQLException {
        ps.setString(1, OrderDetail.CANCELED_NO_CHARGE);
        ps.setString(2, PAID);
        ps.setTimestamp(3, Timestamp.valueOf(from.atStartOfDay()));
        ps.setTimestamp(4, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
    }
}
