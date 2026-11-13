package model;

import java.io.Serializable;

/**
 * A-05 売上管理画面の集計表の 1 行
 * 日別・曜日別・月別は 来店数・客数・売上金額、商品別は 数量・売上金額 を使う
 */
public class SalesRow implements Serializable {

    private final String label;     // 10/01・月・2026/10・商品名(サイズ)
    private int  visits;            // 来店数(会計した注文の数)
    private int  guests;            // 客数
    private int  quantity;          // 数量(商品別)
    private long amount;            // 売上金額
    private int  barPercent;        // 棒グラフの長さ(いちばん大きい行を 100 とする)

    public SalesRow(String label) {
        this.label = label;
    }

    public SalesRow(String label, int visits, int guests, int quantity, long amount) {
        this.label    = label;
        this.visits   = visits;
        this.guests   = guests;
        this.quantity = quantity;
        this.amount   = amount;
    }

    /** 同じ行に足し込む(日別の 0 件の日を埋めるときなどに使う) */
    public void add(int visits, int guests, long amount) {
        this.visits += visits;
        this.guests += guests;
        this.amount += amount;
    }

    public String getLabel() {
        return label;
    }

    public int getVisits() {
        return visits;
    }

    public int getGuests() {
        return guests;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getAmount() {
        return amount;
    }

    public int getBarPercent() {
        return barPercent;
    }

    public void setBarPercent(int barPercent) {
        this.barPercent = barPercent;
    }
}
