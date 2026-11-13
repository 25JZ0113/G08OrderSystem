package model;

import java.io.Serializable;

/**
 * H-01 席状況画面の 1 テーブル分
 * テーブル(TABLES)の状態と、利用中のときは今の来店(未会計の注文)の人数を持つ
 */
public class TableSeat implements Serializable {

    private int     tableId;      // TABLES.ID
    private int     tableNo;      // テーブル番号
    private String  status;       // テーブル状態(空席/利用中/準備中)
    private Integer guestCount;   // 人数(未会計の注文がないときは null)

    public TableSeat() {}

    public TableSeat(int tableId, int tableNo, String status, Integer guestCount) {
        this.tableId    = tableId;
        this.tableNo    = tableNo;
        this.status     = status;
        this.guestCount = guestCount;
    }

    /** 利用中か */
    public boolean isInUse() {
        return TableTerminal.STATUS_IN_USE.equals(status);
    }

    /** 準備中(清掃待ち)か */
    public boolean isPreparing() {
        return TableTerminal.STATUS_PREPARING.equals(status);
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public int getTableNo() {
        return tableNo;
    }

    public void setTableNo(int tableNo) {
        this.tableNo = tableNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }
}
