package com.agrovet.farmcare.service;

import com.agrovet.farmcare.dao.SalesDao;
import com.agrovet.farmcare.models.Sales;
import com.agrovet.farmcare.models.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesService {

    @Autowired
    private SalesDao salesDao;

    @Autowired
    private StockService stockService;

    public Sales recordSale(Sales sale) throws SQLException {
        // Check if stock is available
        List<Stock> stocks = stockService.searchStock(sale.getProductName());
        if (stocks.isEmpty()) {
            throw new SQLException("Product not found: " + sale.getProductName());
        }
        Stock stock = stocks.get(0); // Assuming first match
        if (stock.getQuantity() < sale.getQuantity()) {
            throw new SQLException("Insufficient stock for product: " + sale.getProductName());
        }
        // Record the sale
        Sales recordedSale = salesDao.save(sale);
        // Update stock quantity
        if (stock.getProductID() == null) {
            throw new SQLException("Stock item has no product_id for product: " + stock.getProductName());
        }
        stockService.updateStockQuantity(stock.getProductID(), -sale.getQuantity());
        return recordedSale;
    }

    public Sales getSaleById(int salesID) throws SQLException {
        Sales sale = new Sales();
        sale.setSalesID(salesID);
        return salesDao.get(sale);
    }

    public List<Sales> getAllSales() throws SQLException {
        return salesDao.getAll();
    }

    public Sales updateSale(Sales sale, String[] params) throws SQLException {
        return salesDao.update(sale, params);
    }

    public void deleteSale(int salesID) throws SQLException {
        salesDao.delete(String.valueOf(salesID));
    }

    // Billing and transaction features
    public class Bill {
        private String customerName;
        private List<Sales> items;
        private double totalAmount;
        private Date billDate;

        public Bill(String customerName, List<Sales> items, double totalAmount) {
            this.customerName = customerName;
            this.items = items;
            this.totalAmount = totalAmount;
            this.billDate = new Date();
        }

        public String getCustomerName() { return customerName; }
        public List<Sales> getItems() { return items; }
        public double getTotalAmount() { return totalAmount; }
        public Date getBillDate() { return billDate; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Bill for Customer: ").append(customerName).append("\n");
            sb.append("Date: ").append(billDate).append("\n");
            sb.append("Items:\n");
            for (Sales item : items) {
                sb.append("- ").append(item.getProductName())
                  .append(" x").append(item.getQuantity())
                  .append(" @ ").append(item.getUnitPrice())
                  .append(" = ").append(item.getTotal()).append("\n");
            }
            sb.append("Total: ").append(totalAmount);
            return sb.toString();
        }
    }

    public Bill processCustomerBill(String username, String customerName, List<Map<String, Object>> items) throws SQLException {
        return processCustomerBill(username, customerName, items, new Date());
    }

    public Bill processCustomerBill(String username, String customerName, List<Map<String, Object>> items, Date saleDate) throws SQLException {
        List<Sales> salesItems = new ArrayList<>();
        double total = 0.0;
        Date effectiveDate = saleDate == null ? new Date() : saleDate;

        for (Map<String, Object> item : items) {
            String productName = (String) item.get("productName");
            int quantity = (Integer) item.get("quantity");

            // Find product in stock
            List<Stock> stocks = stockService.searchStock(productName);
            if (stocks.isEmpty()) {
                throw new SQLException("Product not found: " + productName);
            }
            Stock stock = stocks.get(0);
            if (stock.getQuantity() < quantity) {
                throw new SQLException("Insufficient stock for: " + productName);
            }
            if (stock.getProductID() == null) {
                throw new SQLException("Stock item has no product_id for product: " + stock.getProductName());
            }

            double unitPrice = stock.getUnitSp(); // Use selling price
            double itemTotal = unitPrice * quantity;

            Sales sale = new Sales();
            sale.setUsername(username);
            sale.setCustomerName(customerName);
            sale.setProductName(productName);
            sale.setUnitPrice(unitPrice);
            sale.setQuantity(quantity);
            sale.setTotal(itemTotal);
            sale.setSaleDate(effectiveDate);

            // Record sale and update stock
            Sales recorded = salesDao.save(sale);
            stockService.updateStockQuantity(stock.getProductID(), -quantity);
            salesItems.add(recorded);
            total += itemTotal;
        }

        return new Bill(customerName, salesItems, total);
    }

    // Sales tracking and reporting
    public List<Sales> getSalesByCustomer(String customerName) throws SQLException {
        List<Sales> allSales = salesDao.getAll();
        return allSales.stream()
                .filter(sale -> customerName.equalsIgnoreCase(sale.getCustomerName()))
                .collect(Collectors.toList());
    }

    public List<Sales> getSalesByUser(String username) throws SQLException {
        List<Sales> allSales = salesDao.getAll();
        return allSales.stream()
                .filter(sale -> username.equals(sale.getUsername()))
                .collect(Collectors.toList());
    }

    public List<Sales> getSalesByProduct(String productName) throws SQLException {
        List<Sales> allSales = salesDao.getAll();
        return allSales.stream()
                .filter(sale -> productName.equalsIgnoreCase(sale.getProductName()))
                .collect(Collectors.toList());
    }

    public double getTotalSalesRevenue() throws SQLException {
        List<Sales> allSales = salesDao.getAll();
        return allSales.stream()
                .mapToDouble(Sales::getTotal)
                .sum();
    }

    public double getTotalSalesRevenueByDateRange(LocalDate from, LocalDate to) throws SQLException {
        return getTotalSalesRevenueByUserAndDateRange(null, from, to);
    }

    public double getTotalSalesRevenueByUserAndDateRange(String username, LocalDate from, LocalDate to) throws SQLException {
        if (from == null || to == null) return 0.0;
        LocalDate start = from.isAfter(to) ? to : from;
        LocalDate end = from.isAfter(to) ? from : to;

        List<Sales> allSales = salesDao.getAll();
        return allSales.stream()
                .filter(s -> username == null || username.equals(s.getUsername()))
                .filter(s -> withinRange(s.getSaleDate(), start, end))
                .mapToDouble(Sales::getTotal)
                .sum();
    }

    public int getTotalItemsSold() throws SQLException {
        List<Sales> allSales = salesDao.getAll();
        return allSales.stream()
                .mapToInt(Sales::getQuantity)
                .sum();
    }

    public Map<String, Double> getSalesByCategory() throws SQLException {
        List<Sales> allSales = salesDao.getAll();
        Map<String, Double> categorySales = new HashMap<>();

        for (Sales sale : allSales) {
            // Find category from stock
            List<Stock> stocks = stockService.searchStock(sale.getProductName());
            if (!stocks.isEmpty()) {
                String category = stocks.get(0).getCategory();
                categorySales.put(category, categorySales.getOrDefault(category, 0.0) + sale.getTotal());
            }
        }
        return categorySales;
    }

    public List<String> getTopSellingProducts(int limit) throws SQLException {
        List<Sales> allSales = salesDao.getAll();
        Map<String, Integer> productQuantities = new HashMap<>();

        for (Sales sale : allSales) {
            productQuantities.put(sale.getProductName(),
                productQuantities.getOrDefault(sale.getProductName(), 0) + sale.getQuantity());
        }

        return productQuantities.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static boolean withinRange(Date date, LocalDate from, LocalDate to) {
        if (date == null || from == null || to == null) return false;
        LocalDate d = toLocalDate(date);
        return (d.isEqual(from) || d.isAfter(from)) && (d.isEqual(to) || d.isBefore(to));
    }

    private static LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date sd) {
            return sd.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
