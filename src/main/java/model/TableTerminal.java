package model;

import java.io.Serializable;

/**
 * テーブル端末の情報(TERMINAL_SETTINGS と TABLES を結合したもの)
 * 端末コード → どのテーブルか → そのテーブルの状態 をまとめて持つ
 */
public class TableTerminal implements Serializable {

    /** テーブル状態 */
    public static final String STATUS_VACANT    = "空席";
    public static final String STATUS_IN_USE    = "利用中";
    public static final String STATUS_PREPARING = "準備中";

    private String code;         // 端末コード(例: TABLE-05)
    private String name;         // 端末名(例: テーブル5)
    private int    tableId;         // TABLES.ID
    private int    tableNo;         // テーブル番号
    private String tableStatus;  // テーブル状態(空席/利用中/準備中)

    public TableTerminal() {}

    public TableTerminal(String code, String name, int tableId, int tableNo, String tableStatus) {
        this.code        = code;
        this.name        = name;
        this.tableId     = tableId;
        this.tableNo     = tableNo;
        this.tableStatus = tableStatus;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getTableStatus() {
        return tableStatus;
    }

    public void setTableStatus(String tableStatus) {
        this.tableStatus = tableStatus;
    }
}
