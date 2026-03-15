package com.agrovet.farmcare.models;

import java.util.Date;

public class Purchases {
    private int purchaseID;
    private String username;
    private String productName;
    private int quantity;
    private double BuyingPrice;
    private String supplierName;
    private Date purchaseDate;

    public  Purchases(int purchaseID, String username, String productName, int quantity, double BuyingPrice, String supplierName,  Date purchaseDate) {
        this.purchaseID = purchaseID;
        this.username = username;
        this.productName = productName;
        this.quantity = quantity;
        this.BuyingPrice = BuyingPrice;
        this.supplierName = supplierName;
        this.purchaseDate = purchaseDate;
    }
    public Purchases() {}

    public int getPurchaseID() {
        return purchaseID;
    }
    public void setPurchaseID(int purchaseID) {
        this.purchaseID = purchaseID;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public double getBuyingPrice() {
        return BuyingPrice;
    }
    public void setBuyingPrice(double BuyingPrice) {
        this.BuyingPrice = BuyingPrice;
    }
    public String getSupplierName() {
        return supplierName;
    }
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }
    public Date getPurchaseDate() {
        return purchaseDate;
    }
    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public  String toString(){
        return "Purchases{" +
                "username='" + username + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", BuyingPrice=" + BuyingPrice +
                ", supplierName='" + supplierName + '\'' +
                ", purchaseDate=" + purchaseDate +
                '}';
    }
}
