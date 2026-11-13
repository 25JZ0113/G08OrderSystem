package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * カートの 1 行(商品 1 つ + 付けたトッピング + 数量)
 * 金額は DB から取った Product の価格で計算する(画面から送られた金額は使わない)
 */
public class CartItem implements Serializable {

    private int           lineId;                                  // カート内の行番号(変更・削除で使う)
    private Product       product;                             // 注文する商品(サイズ違いは別の商品)
    private List<Product> toppings = new ArrayList<>();  // 付けたトッピング(ID 順)
    private int           quantity;

    public CartItem() {}

    public CartItem(Product product, List<Product> toppings, int quantity) {
        this.product  = product;
        this.toppings = toppings;
        this.quantity = quantity;
    }

    /** 1 個あたりの金額(商品 + トッピング) */
    public int getUnitPrice() {
        int price = product.getPrice();
        for (Product t : toppings) {
            price += t.getPrice();
        }
        return price;
    }

    /** この行の小計(1 個あたりの金額 × 数量) */
    public int getSubtotal() {
        return getUnitPrice() * quantity;
    }

    /** 商品とトッピングの組み合わせが同じか(同じならカートでは 1 行にまとめる) */
    public boolean isSameAs(CartItem other) {
        return product.getId() == other.product.getId()
                && getToppingIds().equals(other.getToppingIds());
    }

    /** トッピングの ID 一覧(画面でチェック済みにするときにも使う) */
    public List<Integer> getToppingIds() {
        List<Integer> ids = new ArrayList<>();
        for (Product t : toppings) {
            ids.add(t.getId());
        }
        return ids;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public List<Product> getToppings() {
        return toppings;
    }

    public void setToppings(List<Product> toppings) {
        this.toppings = toppings;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
