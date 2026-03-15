package com.agrovet.farmcare.models;

import java.util.Date;

public class Sales {
    private int salesID;
    private String username;
    private String customerName;
    private String productName;
    private double unitPrice;
    private int quantity;
    private double total;
    private Date saleDate;

    public Sales(int salesID, String username, String customerName, String productName, double unitPrice, int quantity, double total, Date saleDate) {
        this.salesID = salesID;
        this.username = username;
        this.customerName = customerName;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.total = total;
        this.saleDate = saleDate;
    }
    public Sales(){}

    public int getSalesID() {
        return salesID;
    }
    public void setSalesID(int salesID) {
        this.salesID = salesID;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public double getUnitPrice() {
        return unitPrice;
    }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }
    public int getQuantity() {
        return quantity;
    }
    public double getTotal() {
        return total;
    }
    public void setTotal(double total) {
        this.total = total;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Date getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(Date saleDate) {
        this.saleDate = saleDate;
    }

    public String toString(){
        return username + " " + customerName + " " + productName + " " + quantity + " " + total + " " + saleDate;
    }
}
