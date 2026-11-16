package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 画面表示用の注文明細 1 行
 * 商品の明細と、それに付いたトッピングの明細をまとめて持つ
 * (キッチン・ホール・会計・注文状況の画面では、トッピングを元の商品とまとめて表示する)
 */
public class OrderLine implements Serializable {

    private OrderDetail       detail;                              // 商品の明細
    private List<OrderDetail> toppings = new ArrayList<>();  // 付いたトッピングの明細

    public OrderLine(OrderDetail detail) {
        this.detail = detail;
    }

    public void addTopping(OrderDetail topping) {
        toppings.add(topping);
    }

    public OrderDetail getDetail() {
        return detail;
    }

    public List<OrderDetail> getToppings() {
        return toppings;
    }

    /**
     * この行の金額(商品とトッピングの 販売価格 × 数量 の合計)。T-05 注文履歴で使う
     * キャンセル(請求なし)の明細は 0 円、キャンセル(請求あり)の明細は請求する
     * (ORDERS.TOTAL_AMOUNT と同じ考え方)
     */
    public int getAmount() {
        int amount = amountOf(detail);
        for (OrderDetail t : toppings) {
            amount += amountOf(t);
        }
        return amount;
    }

    private static int amountOf(OrderDetail d) {
        return d.isNoCharge()
                ? 0
                : d.getSalePrice() * d.getQuantity();
    }
}
