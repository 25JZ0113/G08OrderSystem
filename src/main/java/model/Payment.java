package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * R-03 で確定した会計の内容。確定後の画面とレシートに出すためにセッションに入れる
 * (預かり金額・おつりは DB に保存しないので、ここで持つ)
 */
public class Payment implements Serializable {

    private final Bill          bill;
    private final String        method;     // 支払方法
    private final Integer       received;   // 預かり金額(現金のときだけ)
    private final LocalDateTime paidAt;     // 会計日時(レシート用)

    public Payment(Bill bill, String method, Integer received) {
        this.bill     = bill;
        this.method   = method;
        this.received = received;
        this.paidAt   = LocalDateTime.now();
    }

    public Bill getBill() {
        return bill;
    }

    public String getMethod() {
        return method;
    }

    public Integer getReceived() {
        return received;
    }

    /** おつり(現金のときだけ) */
    public Integer getChange() {
        return (received == null) ? null : received - bill.getTotal();
    }

    /** レシートに出す会計日時(JSP の fmt:formatDate で使えるように java.util.Date で返す) */
    public java.util.Date getPaidDate() {
        return java.sql.Timestamp.valueOf(paidAt);
    }
}
