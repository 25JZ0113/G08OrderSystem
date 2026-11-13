package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import model.TableTerminal;
import util.DBManager;

/**
 * 端末設定(TERMINAL_SETTINGS)へのアクセス
 */
public class TerminalDAO {

    /** 端末種別「テーブル」 */
    public static final String TYPE_TABLE = "テーブル";

    /**
     * 端末コードからテーブル端末の情報を取得する
     * テーブル端末でない・テーブルが設定されていない場合は null
     */
    public TableTerminal findTableTerminal(String code) throws SQLException {
        String sql = SELECT_TABLE_TERMINAL + """
                WHERE ts.code = ?
                  AND ts.type = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, TYPE_TABLE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toTableTerminal(rs) : null;
            }
        }
    }

    /**
     * S-02 テーブル番号変更:テーブル番号からテーブル端末の情報を取得する(なければ null)
     */
    public TableTerminal findTableTerminalByTableNo(int tableNo) throws SQLException {
        String sql = SELECT_TABLE_TERMINAL + """
                WHERE t.table_no = ?
                  AND ts.type = ?
                """;

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tableNo);
            ps.setString(2, TYPE_TABLE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toTableTerminal(rs) : null;
            }
        }
    }

    /** テーブル端末の SELECT(端末設定 + テーブル) */
    private static final String SELECT_TABLE_TERMINAL =
            """
            SELECT
                ts.code     AS tscode,
                ts.name     AS tsname,
                t.id        AS tid,
                t.table_no  AS ttable_no,
                t.status    AS tstatus
            FROM
                terminal_settings ts
                JOIN tables t ON ts.table_id = t.id
            """;

    private TableTerminal toTableTerminal(ResultSet rs) throws SQLException {
        return new TableTerminal(
                rs.getString("tscode"),
                rs.getString("tsname"),
                rs.getInt("tid"),
                rs.getInt("ttable_no"),
                rs.getString("tstatus"));
    }
}
