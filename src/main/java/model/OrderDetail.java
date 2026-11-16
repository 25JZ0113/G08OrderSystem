package model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 注文明細(ORDER_DETAILS)
 * トッピングも 1 件の明細として登録し、付けた商品の明細 ID を originalOrderDetailId に入れる
 */
public class OrderDetail implements Serializable {

    /** 注文状態 */
    public static final String RECEIVED           = "注文受付済み";
    public static final String COOKING            = "調理中";
    public static final String COOKED             = "調理済み(提供待ち)";
    public static final String SERVED             = "提供済み";
    public static final String CANCELED_NO_CHARGE = "キャンセル(請求なし)";
    public static final String CANCELED_CHARGE    = "キャンセル(請求あり)";

    /**
     * 会計時に未提供の商品を取り消したときの注文状態(設計書 12 章)
     *   注文受付済み              → キャンセル(請求なし)。合計金額に含めない
     *   調理中・調理済み(提供待ち) → キャンセル(請求あり)。合計金額に含める
     *
     * @return 取り消し後の注文状態。取り消せない状態(提供済み・取り消し済み)のときは null
     */
    public static String canceledStatusOf(String orderStatus) {
        if (RECEIVED.equals(orderStatus)) {
            return CANCELED_NO_CHARGE;
        }
        if (COOKING.equals(orderStatus) || COOKED.equals(orderStatus)) {
            return CANCELED_CHARGE;
        }
        return null;
    }

    private int       id;
    private int       orderId;
    private int       productId;
    private Integer   originalOrderDetailId;   // トッピングのときだけ入る(商品のときは null)
    private int       quantity;
    private int       salePrice;                   // 注文時の販売価格
    private String    orderStatus;
    private Timestamp orderedDatetime;
    private Timestamp cookedDatetime;          // 調理済み(提供待ち)にした日時(調理前は null)
    private Integer   userId;                  // 取り消した社員(取り消したときだけ)
    private Timestamp canceledDatetime;

    // 画面表示用(PRODUCTS から結合して取る)
    private String productName;
    private String sizeName;
    // 画面表示用(ORDERS → TABLES から結合して取る。キッチン・ホールの画面で使う)
    private int    tableNo;
    // 画面表示用(PRODUCTS.TAX_RATE。R-03 会計の内消費税の計算で使う)
    private int    taxRate;

    public OrderDetail() {}

    public int getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(int taxRate) {
        this.taxRate = taxRate;
    }

    /** 未提供か(注文受付済み・調理中・調理済み(提供待ち)) */
    public boolean isUnserved() {
        return RECEIVED.equals(orderStatus) || COOKING.equals(orderStatus) || COOKED.equals(orderStatus);
    }

    public int getTableNo() {
        return tableNo;
    }

    public void setTableNo(int tableNo) {
        this.tableNo = tableNo;
    }

    /** 取り消し済みか(キャンセル(請求なし)・キャンセル(請求あり)) */
    public boolean isCanceled() {
        return CANCELED_NO_CHARGE.equals(orderStatus) || CANCELED_CHARGE.equals(orderStatus);
    }

    /** 料金を請求しない明細か(キャンセル(請求なし))。合計金額・売上に含めない */
    public boolean isNoCharge() {
        return CANCELED_NO_CHARGE.equals(orderStatus);
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public Integer getOriginalOrderDetailId() {
        return originalOrderDetailId;
    }

    public void setOriginalOrderDetailId(Integer originalOrderDetailId) {
        this.originalOrderDetailId = originalOrderDetailId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(int salePrice) {
        this.salePrice = salePrice;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Timestamp getOrderedDatetime() {
        return orderedDatetime;
    }

    public void setOrderedDatetime(Timestamp orderedDatetime) {
        this.orderedDatetime = orderedDatetime;
    }

    public Timestamp getCookedDatetime() {
        return cookedDatetime;
    }

    public void setCookedDatetime(Timestamp cookedDatetime) {
        this.cookedDatetime = cookedDatetime;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Timestamp getCanceledDatetime() {
        return canceledDatetime;
    }

    public void setCanceledDatetime(Timestamp canceledDatetime) {
        this.canceledDatetime = canceledDatetime;
    }
}
