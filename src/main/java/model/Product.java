package model;

import java.io.Serializable;

/**
 * Step3 商品(PRODUCTS テーブル)の JavaBeans
 * 一覧表示用にカテゴリ名(CATEGORIES.NAME)も持つ
 */
public class Product implements Serializable {

    private int    id;
    private int    categoryId;
    private String categoryName;
    private String name;
    private String sizeName;
    private int    price;
    private int    taxRate;
    private String salesStatus;
    private String description;
    private String imageUrl;

    public Product() {}

    public Product(int id, int categoryId, String categoryName, String name, String sizeName,
            int price, int taxRate, String salesStatus, String description, String imageUrl) {
        this.id           = id;
        this.categoryId   = categoryId;
        this.categoryName = categoryName;
        this.name         = name;
        this.sizeName     = sizeName;
        this.price        = price;
        this.taxRate      = taxRate;
        this.salesStatus  = salesStatus;
        this.description  = description;
        this.imageUrl     = imageUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(int taxRate) {
        this.taxRate = taxRate;
    }

    public String getSalesStatus() {
        return salesStatus;
    }

    public void setSalesStatus(String salesStatus) {
        this.salesStatus = salesStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
