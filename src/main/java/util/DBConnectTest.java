package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * Step1・Step2・Step2b の動作確認用(main メソッドで実行する)
 * step1  接続成功・失敗の確認
 * step2  PreparedStatement でパラメータ付き SELECT → ResultSet → コンソール出力(カテゴリ別商品一覧)
 * step2b JOIN を含んだ SQL と、Oracle の DATE 型の扱い方(Timestamp で取り出してフォーマットする)
 */
public class DBConnectTest {

    // stepごとの動作確認処理 呼び出し用 main
    public static void main(String[] args) {
        step1();    // 接続確認
        step2();    // カテゴリ別商品一覧
        step2b();   // 注文明細一覧(JOIN・DATE 型)
    }

    /**
     * step1 Oracle への接続確認
     */
    public static void step1() {
        System.out.println("\t\t\t--------------------------step1");
        // try-with-resources を利用して、処理終了時に接続を自動で閉じる
        try (Connection con = DBManager.getConnection()) {
            // 接続に成功した場合のメッセージ
            System.out.println("Oracleデータベースへの接続に成功しました。");
            System.out.println("データベース名：" + con.getMetaData().getDatabaseProductName());
            System.out.println("データベースのバージョン：" + con.getMetaData().getDatabaseProductVersion());
            System.out.println("接続ユーザー：" + con.getMetaData().getUserName());
        } catch (SQLException e) {
            printError(e);
        }
    }

    /**
     * step2 Oracle から商品一覧取得(パラメータを含む SQL。? に値を設定する)
     */
    public static void step2() {
        System.out.println("\t\t\t--------------------------step2");
        // 実行するSQL文 テキストブロック(""" の後ろには何も書かない)
        String sql =
                """
                SELECT
                    p.id        AS pid,
                    p.name      AS pname,
                    p.size_name AS psize_name,
                    p.price     AS pprice,
                    c.name      AS cname
                FROM
                    products p
                    JOIN categories c ON p.category_id = c.id
                WHERE
                    p.sales_status = ?
                ORDER BY c.id, p.id
                """;
        System.out.println("sql\n" + sql);   // SQL文の確認表示

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "販売中");

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("商品一覧");
                System.out.println("--------------------------------");

                boolean found = false;   // 検索結果が 1 件もない場合の判定
                while (rs.next()) {
                    found = true;
                    int    id       = rs.getInt("pid");
                    String category = rs.getString("cname");
                    String name     = rs.getString("pname");
                    String size     = rs.getString("psize_name"); // NULL の場合あり
                    int    price    = rs.getInt("pprice");

                    System.out.println("商品ID：" + id);
                    System.out.println("カテゴリ：" + category);
                    System.out.println("商品名：" + name + (size != null ? "(" + size + ")" : ""));
                    System.out.println("価格　：" + price + "円");
                    System.out.println("--------------------------------");
                }
                if (!found) {
                    System.out.println("商品データが登録されていません。");
                }
            }
        } catch (SQLException e) {
            printError(e);
        }
    }

    /**
     * step2b JOIN を含んだ SQL の実行と、Oracle の DATE 型の扱い方
     * G08 の PRODUCTS には日時の列がないので、注文明細の「注文日時」(ORDERED_DATETIME)で確認する
     * DATE 型は SQL で TO_CHAR せず、Java で Timestamp 型として取り出す
     */
    public static void step2b() {
        System.out.println("\t\t\t--------------------------step2b");
        String sql =
                """
                SELECT
                    d.id               AS did,
                    t.table_no         AS ttable_no,
                    p.name             AS pname,
                    p.size_name        AS psize_name,
                    d.quantity         AS dquantity,
                    d.order_status     AS dorder_status,
                    d.ordered_datetime AS dordered_datetime
                FROM
                    order_details d
                    JOIN products p ON d.product_id = p.id
                    JOIN orders o   ON d.order_id = o.id
                    JOIN tables t   ON o.table_id = t.id
                ORDER BY d.ordered_datetime, d.id
                """;
        System.out.println("sql\n" + sql);

        // Timestamp 型の書式設定(大文字・小文字に注意。MM は月、mm は分)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        try (Connection con = DBManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            System.out.println("注文明細一覧");
            System.out.println("--------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                String    size        = rs.getString("psize_name");
                Timestamp orderedDate = rs.getTimestamp("dordered_datetime");   // DATE → Timestamp

                System.out.println("明細ID：" + rs.getInt("did"));
                System.out.println("テーブル：" + rs.getInt("ttable_no"));
                System.out.println("商品名：" + rs.getString("pname")
                        + (size != null ? "(" + size + ")" : ""));
                System.out.println("数量　：" + rs.getInt("dquantity"));
                System.out.println("状態　：" + rs.getString("dorder_status"));
                // ① Timestamp 型のまま出力(ナノ秒まで持つので最後に .0 が付く)
                System.out.println("注文日時：" + orderedDate);
                // ② フォーマッターで好みの書式に変換して出力
                System.out.println("注文日時：" + orderedDate.toLocalDateTime().format(formatter));
                System.out.println("--------------------------------");
            }
            if (!found) {
                System.out.println("注文明細データが登録されていません。");
            }
        } catch (SQLException e) {
            printError(e);
        }
    }

    /** 接続・SQL 実行に失敗した場合のエラー処理 */
    private static void printError(SQLException e) {
        System.out.println("データベース処理でエラーが発生しました。");
        System.out.println("エラーコード：" + e.getErrorCode());
        System.out.println("エラー内容：" + e.getMessage());
        e.printStackTrace();
    }
}
