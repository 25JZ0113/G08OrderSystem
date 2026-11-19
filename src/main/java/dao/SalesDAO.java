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
            rows.put(d, new SalesRow(String.format("%02d/%02d", d.getMonthValue(), d.getDayOfMonth())));
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
        for (String n : names) {
            rows.add(new SalesRow(n));
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
            rows.put(m, new SalesRow(String.format("%d/%02d", m.getYear(), m.getMonthValue())));
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
                    rows.add(new SalesRow(label, 0, 0, rs.getInt("qty"), rs.getLong("amount")));
                }
            }
        }
        return rows;
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
