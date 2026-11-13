package dao;

import java.sql.SQLException;
import java.util.List;

import model.Product;

/**
 * Step3・Step4 の動作確認用(main メソッドで実行する)
 * DAO から Product(List)を受け取り、Bean の getter で値を出力する
 * ・Products 全件検索
 * ・商品 ID を指定して検索
 * ・商品名を指定して検索
 */
public class ProductDAOTest {

    public static void main(String[] args) {
        ProductDAO dao = new ProductDAO();
        try {
            System.out.println("\t\t\t--------------------------全件検索");
            List<Product> all = dao.findAll();
            System.out.println("取得件数：" + all.size() + "件");
            for (Product p : all) {
                print(p);
            }

            System.out.println("\t\t\t--------------------------商品IDを指定して検索");
            Product one = dao.findById(1);
            if (one == null) {
                System.out.println("商品データが登録されていません。");
            }
            else {
                print(one);
            }

            System.out.println("\t\t\t--------------------------商品名を指定して検索");
            List<Product> found = dao.findByName("ラテ");
            if (found.isEmpty()) {
                System.out.println("商品データが登録されていません。");
            }
            for (Product p : found) {
                print(p);
            }
        } catch (SQLException e) {
            // SQL実行時やデータベース接続時のエラー処理
            System.out.println("データベース処理でエラーが発生しました。");
            System.out.println("エラーコード：" + e.getErrorCode());
            System.out.println("エラー内容：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 商品 1 件を表示する */
    private static void print(Product p) {
        System.out.println("商品ID：" + p.getId());
        System.out.println("カテゴリ：" + p.getCategoryName());
        System.out.println("商品名：" + p.getName()
                + (p.getSizeName() != null ? "(" + p.getSizeName() + ")" : ""));
        System.out.println("価格　：" + p.getPrice() + "円");
        System.out.println("--------------------------------");
    }
}
