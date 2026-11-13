package model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 注文(ORDERS)。1 回の来店・会計の単位
 * DATE 型の列は Timestamp で持つ(SQL で TO_CHAR しない)
 */
public class Order implements Serializable {

    /** 会計状態 */
    public static final String UNPAID = "未会計";
    public static final String PAID   = "会計済み";

    private int       id;
    private int       tableId;
    private int       guestCount;
    private Timestamp visitDatetime;
    private Timestamp paymentDatetime;
    private String    paymentStatus;
    private String    paymentMethod;
    private int       totalAmount;

    // 画面表示用(TABLES から結合して取る。レジの画面で使う)
    private int       tableNo;

    /** 支払方法 */
    public static final String METHOD_CASH   = "現金";
    public static final String METHOD_CREDIT = "クレジットカード";
    public static final String METHOD_EMONEY = "電子マネー";

    public Order() {}

    public int getTableNo() {
        return tableNo;
    }

    public void setTableNo(int tableNo) {
        this.tableNo = tableNo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public Timestamp getVisitDatetime() {
        return visitDatetime;
    }

    public void setVisitDatetime(Timestamp visitDatetime) {
        this.visitDatetime = visitDatetime;
    }

    public Timestamp getPaymentDatetime() {
        return paymentDatetime;
    }

    public void setPaymentDatetime(Timestamp paymentDatetime) {
        this.paymentDatetime = paymentDatetime;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }
}
