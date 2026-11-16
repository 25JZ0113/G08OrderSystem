package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * R-03 会計画面・レシートに出す内容
 * キャンセル(請求なし)の商品は明細に出さず、合計にも含めない
 * キャンセル(請求あり)の商品は明細に出し(状態も表示する)、合計に含める
 * 価格は税込なので、合計金額の中に含まれる消費税(内消費税)を税率ごとに計算する
 */
public class Bill implements Serializable {

    private final Order           order;
    private final List<OrderLine> lines    = new ArrayList<>();   // 会計に出す行
    private int                   total;                         // 合計(税込)
    private int                   tax;                           // 内消費税

    public Bill(Order order, List<OrderLine> allLines) {
        this.order = order;

        // 税率ごとの金額(トッピングの税率が商品と違っても正しく計算できるように明細ごとに足す)
        Map<Integer, Integer> amountByRate = new TreeMap<>();
        for (OrderLine line : allLines) {
            if (line.getDetail().isNoCharge()) {
                continue;   // キャンセル(請求なし)の商品は請求しない
            }
            lines.add(line);
            add(amountByRate, line.getDetail());
            for (OrderDetail t : line.getToppings()) {
                add(amountByRate, t);
            }
        }

        // 内消費税 = 税込金額 × 税率 ÷ (100 + 税率)。税率ごとに 1 円未満を切り捨て
        for (Map.Entry<Integer, Integer> e : amountByRate.entrySet()) {
            int rate   = e.getKey();
            int amount = e.getValue();
            total += amount;
            tax   += amount * rate / (100 + rate);
        }
    }

    private static void add(Map<Integer, Integer> amountByRate, OrderDetail d) {
        if (d.isNoCharge()) {
            return;
        }
        amountByRate.merge(d.getTaxRate(), d.getSalePrice() * d.getQuantity(), Integer::sum);
    }

    public Order getOrder() {
        return order;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    /** 小計(価格は税込なので合計と同じ) */
    public int getSubtotal() {
        return total;
    }

    public int getTax() {
        return tax;
    }

    public int getTotal() {
        return total;
    }
}
