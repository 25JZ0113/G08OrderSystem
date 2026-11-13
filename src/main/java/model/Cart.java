package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * カート(注文を確定するまでセッションに保存しておく)
 */
public class Cart implements Serializable {

    /** 1 行の数量の上限 */
    public static final int MAX_QUANTITY = 10;

    private List<CartItem> items      = new ArrayList<>();
    private int            nextLineId = 1;

    /**
     * 商品を追加する
     * 同じ商品・同じトッピングの行があれば数量を足す(上限 MAX_QUANTITY)
     */
    public void add(CartItem newItem) {
        for (CartItem item : items) {
            if (item.isSameAs(newItem)) {
                item.setQuantity(
                        Math.min(MAX_QUANTITY, item.getQuantity() + newItem.getQuantity()));
                return;
            }
        }
        newItem.setLineId(nextLineId++);
        items.add(newItem);
    }

    /**
     * 行を置き換える(商品詳細画面でサイズ・トッピング・数量を選び直したとき)
     * 選び直した結果が別の行と同じ組み合わせになったら、その行にまとめる
     */
    public void replace(int lineId, CartItem newItem) {
        CartItem old = find(lineId);
        if (old == null) {
            add(newItem);
            return;
        }
        for (CartItem item : items) {
            if (item != old && item.isSameAs(newItem)) {
                item.setQuantity(
                        Math.min(MAX_QUANTITY, item.getQuantity() + newItem.getQuantity()));
                items.remove(old);
                return;
            }
        }
        old.setProduct(newItem.getProduct());
        old.setToppings(newItem.getToppings());
        old.setQuantity(newItem.getQuantity());
    }

    /** 数量を増減する(1〜MAX_QUANTITY の範囲) */
    public void changeQuantity(int lineId, int diff) {
        CartItem item = find(lineId);
        if (item != null) {
            int q = item.getQuantity() + diff;
            item.setQuantity(Math.max(1, Math.min(MAX_QUANTITY, q)));
        }
    }

    /** 行を削除する */
    public void remove(int lineId) {
        items.removeIf(item -> item.getLineId() == lineId);
    }

    /** 行番号で探す(なければ null) */
    public CartItem find(int lineId) {
        for (CartItem item : items) {
            if (item.getLineId() == lineId) {
                return item;
            }
        }
        return null;
    }

    /** カート内の点数(数量の合計) */
    public int getTotalQuantity() {
        int total = 0;
        for (CartItem item : items) {
            total += item.getQuantity();
        }
        return total;
    }

    /** 合計金額 */
    public int getTotalAmount() {
        int total = 0;
        for (CartItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> getItems() {
        return items;
    }
}
