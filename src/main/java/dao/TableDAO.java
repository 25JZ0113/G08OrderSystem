package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.Order;
import model.TableSeat;
import model.TableTerminal;
import util.DBManager;

/**
 * テーブル(TABLES)へのアクセス
 */
public class TableDAO {

    /**
     * H-01 席状況:全テーブルの状態と、利用中のテーブルの人数をテーブル番号順に取得する
     * 人数は、そのテーブルの未会計の注文(今の来店)から取る。未会計が複数あるときは来店日時が新しいもの
     */
    public List<TableSeat> findSeats() throws SQLException {
        String sql = SELECT_SEAT + """
                ORDER BY t.table_no
                """;
        List<TableSeat> list = new ArrayList<>();

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Order.UNPAID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toSeat(rs));
                }
            }
        }
        return list;
    }

    /**
     * H-02 人数変更:1 テーブル分の状態と人数を取得する(なければ null)
     */
    public TableSeat findSeat(int tableId) throws SQLException {
        String sql = SELECT_SEAT + """
                WHERE t.id = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Order.UNPAID);
            ps.setInt(2, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toSeat(rs) : null;
            }
        }
    }

    /** 席状況の SELECT(1 つ目の ? は「未会計」) */
    private static final String SELECT_SEAT =
            """
            SELECT
                t.id       AS tid,
                t.table_no AS ttable_no,
                t.status   AS tstatus,
                (SELECT MAX(o.guest_count) KEEP (DENSE_RANK LAST ORDER BY o.visit_datetime)
                   FROM orders o
                  WHERE o.table_id = t.id
                    AND o.payment_status = ?) AS oguest_count
            FROM
                tables t
            """;

    private TableSeat toSeat(ResultSet rs) throws SQLException {
        // 人数は NULL のことがある(wasNull は getInt の直後に呼ぶ)
        int     count      = rs.getInt("oguest_count");
        Integer guestCount = rs.wasNull() ? null : count;
        return new TableSeat(
                rs.getInt("tid"),
                rs.getInt("ttable_no"),
                rs.getString("tstatus"),
                guestCount);
    }

    /**
     * 清掃が完了したテーブルを空席にする(準備中 → 空席)
     * 準備中のときだけ変更する(2 回押された・別の端末で変更済みのときは変更しない)
     * 空席になると、テーブル端末は待機画面から人数入力画面に切り替わる
     *
     * @return 変更できたら true
     */
    public boolean finishCleaning(int tableId) throws SQLException {
        String sql =
                """
                UPDATE tables
                   SET status = ?
                 WHERE id = ?
                   AND status = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, TableTerminal.STATUS_VACANT);
            ps.setInt(2, tableId);
            ps.setString(3, TableTerminal.STATUS_PREPARING);
            return ps.executeUpdate() > 0;
        }
    }
}
