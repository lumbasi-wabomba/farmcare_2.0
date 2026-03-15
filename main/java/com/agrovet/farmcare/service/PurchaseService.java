package com.agrovet.farmcare.service;

import com.agrovet.farmcare.dao.PurchasesDao;
import com.agrovet.farmcare.models.Purchases;
import com.agrovet.farmcare.models.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseService {
    @Autowired
    private PurchasesDao purchasesDao;
    @Autowired
    private StockService stockService;

    /**
     * Record a purchase and update stock quantity. If the product is not found in stock,
     * a new stock item will only be created when {@code allowCreateStock} is true.
     */
    public Purchases recordPurchase(Purchases purchase, boolean allowCreateStock) throws SQLException {
        // Record purchase first to obtain its ID, then update stock. If stock update fails, roll back the purchase row.
        Purchases recordedPurchase = purchasesDao.save(purchase);
        try {
            updateStockFromPurchase(purchase, allowCreateStock);
            return recordedPurchase;
        } catch (Exception e) {
            try {
                purchasesDao.delete(String.valueOf(recordedPurchase.getPurchaseID()));
            } catch (Exception ignored) {
                // Best-effort rollback; surface original error.
            }
            if (e instanceof SQLException se) throw se;
            throw new SQLException("Stock update failed after recording purchase", e);
        }
    }

    /**
     * Backwards-compatible default: do not allow creating new stock items from purchases unless explicitly enabled.
     */
    public Purchases recordPurchase(Purchases purchase) throws SQLException {
        return recordPurchase(purchase, false);
    }

    private void updateStockFromPurchase(Purchases purchase, boolean allowCreateStock) throws SQLException {
        String rawName = purchase.getProductName();
        String productName = rawName == null ? "" : rawName.trim();
        if (productName.isEmpty()) {
            throw new SQLException("Product name is required");
        }
        purchase.setProductName(productName);

        Stock stock = stockService.getStockByNameExact(productName);
        if (stock == null) {
            List<Stock> existingStocks = stockService.searchStock(productName);
            if (!existingStocks.isEmpty()) {
                stock = existingStocks.stream()
                        .filter(s -> s.getProductName() != null && s.getProductName().equalsIgnoreCase(productName))
                        .findFirst()
                        .orElse(existingStocks.get(0));
            }
        }

        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + purchase.getQuantity());
            stock.setUnitBp(purchase.getBuyingPrice()); // Update buying price
            String[] params = {
                stock.getProductName(),
                stock.getProductDescription(),
                stock.getCategory(),
                String.valueOf(stock.getUnitBp()),
                String.valueOf(stock.getUnitSp()),
                String.valueOf(stock.getQuantity()),
                String.valueOf(stock.getReorderLevel())
            };
            stockService.updateStock(stock, params);
        } else {
            if (!allowCreateStock) {
                throw new SQLException("Product not found in stock: " + productName + ". Ask an admin/superuser to add it first.");
            }
            // Add new stock with default values - this might need more info, but for now, assume minimal
            Stock newStock = new Stock();
//            newStock.setProductID();
            newStock.setProductName(productName);
            newStock.setProductDescription("Purchased from " + purchase.getSupplierName());
            newStock.setCategory("General"); // Default category
            newStock.setUnitBp(purchase.getBuyingPrice());
            newStock.setUnitSp(purchase.getBuyingPrice() * 1.2); // Default markup
            newStock.setQuantity(purchase.getQuantity());
            newStock.setReorderLevel(10); // Default reorder level
            stockService.addStock(newStock);
        }
    }

//    private String generateProductID(String productName) {
//        // Simple ID generation
//        String name = productName == null ? "" : productName.trim();
//        if (name.isEmpty()) {
//            return "P" + (System.currentTimeMillis() % 1_000_000);
//        }
//        String prefix = name.toUpperCase().replaceAll("\\s+", "");
//        prefix = prefix.substring(0, Math.min(5, prefix.length()));
//        return prefix + (System.currentTimeMillis() % 1000);
//    }

    public Purchases getPurchaseById(int purchaseID) throws SQLException {
        Purchases purchase = new Purchases();
        purchase.setPurchaseID(purchaseID);
        return purchasesDao.get(purchase);
    }

    public List<Purchases> getAllPurchases() throws SQLException {
        return purchasesDao.getAll();
    }

    public Purchases updatePurchase(Purchases purchase, String[] params) throws SQLException {
        return purchasesDao.update(purchase, params);
    }

    public void deletePurchase(int purchaseID) throws SQLException {
        purchasesDao.delete(String.valueOf(purchaseID));
    }

    // Purchase tracking and reporting
    public List<Purchases> getPurchasesByUser(String username) throws SQLException {
        List<Purchases> allPurchases = purchasesDao.getAll();
        return allPurchases.stream()
                .filter(purchase -> username.equals(purchase.getUsername()))
                .collect(Collectors.toList());
    }

    public List<Purchases> getPurchasesBySupplier(String supplierName) throws SQLException {
        List<Purchases> allPurchases = purchasesDao.getAll();
        return allPurchases.stream()
                .filter(purchase -> supplierName.equalsIgnoreCase(purchase.getSupplierName()))
                .collect(Collectors.toList());
    }

    public List<Purchases> getPurchasesByProduct(String productName) throws SQLException {
        List<Purchases> allPurchases = purchasesDao.getAll();
        return allPurchases.stream()
                .filter(purchase -> productName.equalsIgnoreCase(purchase.getProductName()))
                .collect(Collectors.toList());
    }

    public double getTotalPurchaseCost() throws SQLException {
        List<Purchases> allPurchases = purchasesDao.getAll();
        return allPurchases.stream()
                .mapToDouble(purchase -> purchase.getBuyingPrice() * purchase.getQuantity())
                .sum();
    }

    public double getTotalPurchaseCostByDateRange(LocalDate from, LocalDate to) throws SQLException {
        if (from == null || to == null) return 0.0;
        LocalDate start = from.isAfter(to) ? to : from;
        LocalDate end = from.isAfter(to) ? from : to;

        List<Purchases> allPurchases = purchasesDao.getAll();
        return allPurchases.stream()
                .filter(p -> withinRange(p.getPurchaseDate(), start, end))
                .mapToDouble(p -> p.getBuyingPrice() * p.getQuantity())
                .sum();
    }

    public int getTotalItemsPurchased() throws SQLException {
        List<Purchases> allPurchases = purchasesDao.getAll();
        return allPurchases.stream()
                .mapToInt(Purchases::getQuantity)
                .sum();
    }

    public Map<String, Double> getPurchasesBySupplierCost() throws SQLException {
        List<Purchases> allPurchases = purchasesDao.getAll();
        Map<String, Double> supplierCosts = new java.util.HashMap<>();
        for (Purchases purchase : allPurchases) {
            double cost = purchase.getBuyingPrice() * purchase.getQuantity();
            supplierCosts.put(purchase.getSupplierName(), supplierCosts.getOrDefault(purchase.getSupplierName(), 0.0) + cost);
        }
        return supplierCosts;
    }

    public List<String> getAllSuppliers() throws SQLException {
        List<Purchases> allPurchases = purchasesDao.getAll();
        return allPurchases.stream()
                .map(Purchases::getSupplierName)
                .distinct()
                .collect(Collectors.toList());
    }

    // Advanced feature: Bulk purchase processing
    public List<Purchases> processBulkPurchases(String username, List<Map<String, Object>> purchaseItems) throws SQLException {
        List<Purchases> recordedPurchases = new java.util.ArrayList<>();
        for (Map<String, Object> item : purchaseItems) {
            String productName = (String) item.get("productName");
            int quantity = (Integer) item.get("quantity");
            double buyingPrice = (Double) item.get("buyingPrice");
            String supplierName = (String) item.get("supplierName");

            Purchases purchase = new Purchases();
            purchase.setUsername(username);
            purchase.setProductName(productName);
            purchase.setQuantity(quantity);
            purchase.setBuyingPrice(buyingPrice);
            purchase.setSupplierName(supplierName);

            Purchases recorded = recordPurchase(purchase, false);
            recordedPurchases.add(recorded);
        }
        return recordedPurchases;
    }

    private static boolean withinRange(java.util.Date date, LocalDate from, LocalDate to) {
        if (date == null || from == null || to == null) return false;
        LocalDate d = toLocalDate(date);
        return (d.isEqual(from) || d.isAfter(from)) && (d.isEqual(to) || d.isBefore(to));
    }

    private static LocalDate toLocalDate(java.util.Date date) {
        if (date instanceof java.sql.Date sd) {
            return sd.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
