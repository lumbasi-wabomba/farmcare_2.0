package com.agrovet.farmcare.models;

public class Stock {
    private Integer productID;
    private String productName;
    private String productDescription;
    private String category;
    private double UnitBp;
    private double UnitSp;
    private int quantity;
    private int reorderLevel;

    public Stock() {}
    public Stock(Integer productID, String productName, String productDescription, String category, double UnitBp, double UnitSp, int quantity, int reorderLevel) {
        this.productID = productID;
        this.productName = productName;
        this.productDescription = productDescription;
        this.category = category;
        this.UnitBp = UnitBp;
        this.UnitSp = UnitSp;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    public Integer getProductID() {
        return productID;
    }
    public void setProductID(Integer productID) {
        this.productID = productID;
    }
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public String getProductDescription() {
        return productDescription;
    }
    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public double getUnitBp() {
        return UnitBp;
    }
    public void setUnitBp(double unitBp) {
        UnitBp = unitBp;
    }
    public double getUnitSp() {
        return UnitSp;
    }
    public void setUnitSp(double unitSp) {
        UnitSp = unitSp;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public int getReorderLevel() {
        return reorderLevel;
    }
    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public String toString() {
        return "Stock{" +
                "productID='" + productID + '\'' +
                ", productName='" + productName + '\'' +
                ", productDescription='" + productDescription + '\'' +
                ", category='" + category + '\'' +
                ", UnitBp=" + UnitBp +
                ", UnitSp=" + UnitSp +
                ", quantity=" + quantity +
                ", reorderLevel=" + reorderLevel +
                '}';
    }
}
