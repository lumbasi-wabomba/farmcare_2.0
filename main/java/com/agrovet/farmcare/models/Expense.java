package com.agrovet.farmcare.models;

import java.util.Date;

public class Expense {
    private String Username;
    private int ExpenseID;
    private Date ExpenseDate;
    private String ExpenseType;
    private String ExpenseDescription;
    private double Total;

     public Expense(String username, int expenseID, Date expenseDate, String expenseType, String expenseDescription, double total) {
        Username = username;
        ExpenseID = expenseID;
        ExpenseDate = expenseDate;
        ExpenseType = expenseType;
        ExpenseDescription = expenseDescription;
        Total = total;
    }

    public Expense() {}
    public String getUsername() {
        return Username;
    }
    public void setUsername(String username) {
        Username = username;
    }
    public int getExpenseID() {
        return ExpenseID;
    }
    public void setExpenseID(int expenseID) {
        ExpenseID = expenseID;
    }
    public Date getExpenseDate() {
        return ExpenseDate;
    }
    public void setExpenseDate(Date expenseDate) {
        ExpenseDate = expenseDate;
    }
    public String getExpenseType() {
        return ExpenseType;
    }
    public void setExpenseType(String expenseType) {
        ExpenseType = expenseType;
    }
    public String getExpenseDescription() {
        return ExpenseDescription;
    }
    public void setExpenseDescription(String expenseDescription) {
        ExpenseDescription = expenseDescription;
    }
    public double getTotal() {
         return Total;
    }
    public void setTotal(double total) {
         Total = total;
    }

    public String toString(){
         return "Expense{" +
                 "Username='" + Username + '\'' +
                 ", ExpenseID='" + ExpenseID + '\'' +
                 ", ExpenseDate=" + ExpenseDate +
                 ", ExpenseType='" + ExpenseType + '\'' +
                 ", ExpenseDescription='" + ExpenseDescription + '\'' +
                 ", Total=" + Total +
                 '}';
    }


}
