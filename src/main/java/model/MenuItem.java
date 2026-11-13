package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * メニュー一覧の 1 行分
 * 同じ商品名でサイズ違いの商品(PRODUCTS の複数行)を 1 つにまとめる
 * 例: ブレンドコーヒー → S(350円)・M(400円)・L(450円)
 */
public class MenuItem implements Serializable {

    private String        name;
    private List<Product> variants = new ArrayList<>(); // サイズごとの商品(ID 順)

    public MenuItem() {}

    public MenuItem(String name) {
        this.name = name;
    }

    public void addVariant(Product product) {
        variants.add(product);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Product> getVariants() {
        return variants;
    }

    public void setVariants(List<Product> variants) {
        this.variants = variants;
    }

    /** サイズの選択肢があるか(サイズ違いが 2 つ以上、またはサイズ名が入っている) */
    public boolean isHasSizes() {
        return variants.size() > 1 || variants.get(0).getSizeName() != null;
    }

    /** いちばん安い価格(「350円〜」の表示に使う) */
    public int getMinPrice() {
        int min = Integer.MAX_VALUE;
        for (Product p : variants) {
            min = Math.min(min, p.getPrice());
        }
        return min;
    }

    /** 価格がサイズによって変わるか */
    public boolean isPriceVaries() {
        for (Product p : variants) {
            if (p.getPrice() != getMinPrice()) {
                return true;
            }
        }
        return false;
    }

    /** 代表の商品 ID(いちばん小さいサイズ。商品詳細への遷移に使う) */
    public int getFirstProductId() {
        return variants.get(0).getId();
    }
}
