package model;

import java.io.Serializable;
import java.util.List;

/**
 * A-06 売上の注文一覧の 1 件:注文と、その注文明細(トッピングは元の商品の行にまとめる)
 * 取り消した明細(キャンセル(請求なし)・キャンセル(請求あり))も、状態を出すためにそのまま持つ
 */
public class SalesOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Order           order;
    private final List<OrderLine> lines;

    public SalesOrder(Order order, List<OrderLine> lines) {
        this.order = order;
        this.lines = lines;
    }

    public Order getOrder() {
        return order;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}
