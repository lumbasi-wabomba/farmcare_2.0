package com.agrovet.farmcare.service;

import com.agrovet.farmcare.dao.StockDao;
import com.agrovet.farmcare.models.Stock;
import com.agrovet.farmcare.models.StockPage;
import com.agrovet.farmcare.models.StockSort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    @Autowired
    private StockDao stockDao;

    // Basic CRUD operations
    public Stock addStock(Stock stock) throws SQLException {
        // Check if product already exists
        if (stock.getProductID() != null) {
            Stock existing = new Stock();
            existing.setProductID(stock.getProductID());
            if (stockDao.get(existing) != null) {
                throw new SQLException("Product with ID " + stock.getProductID() + " already exists");
            }
        }
        return stockDao.save(stock);
    }

    public Stock getStockById(int productID) throws SQLException {
        Stock stock = new Stock();
        stock.setProductID(productID);
        return stockDao.get(stock);
    }

    public Stock getStockByNameExact(String productName) throws SQLException {
        if (productName == null) return null;
        String name = productName.trim();
        if (name.isEmpty()) return null;
        return stockDao.findFirstByNameExact(name);
    }

    public StockPage getStockPage(int page, int pageSize, StockSort sort) throws SQLException {
        int safePageSize = pageSize <= 0 ? 25 : pageSize;
        int totalItems = stockDao.countAll();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safePageSize));
        int safePage = Math.min(Math.max(page, 1), totalPages);
        int offset = (safePage - 1) * safePageSize;

        List<Stock> items = stockDao.getPage(safePageSize, offset, sort == null ? StockSort.QUANTITY_DESC : sort);
        return new StockPage(items, safePage, safePageSize, totalItems, totalPages, sort == null ? StockSort.QUANTITY_DESC : sort);
    }

    public List<Stock> getAllStock() throws SQLException {
        return stockDao.getAll();
    }

    public Stock updateStock(Stock stock, String[] params) throws SQLException {
        return stockDao.update(stock, params);
    }

    public void deleteStock(int productID) throws SQLException {
        stockDao.delete(String.valueOf(productID));
    }

    // Stock monitoring features
    public List<Stock> getLowStockItems() throws SQLException {
        List<Stock> allStock = stockDao.getAll();
        return allStock.stream()
                .filter(stock -> stock.getQuantity() <= stock.getReorderLevel())
                .collect(Collectors.toList());
    }

    public void updateStockQuantity(int productID, int quantityChange) throws SQLException {
        Stock stock = getStockById(productID);
        if (stock == null) {
            throw new SQLException("Product not found: " + productID);
        }
        int newQuantity = stock.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new SQLException("Insufficient stock for product: " + productID);
        }
        stock.setQuantity(newQuantity);
        // Update only quantity
        String[] params = {
            stock.getProductName(),
            stock.getProductDescription(),
            stock.getCategory(),
            String.valueOf(stock.getUnitBp()),
            String.valueOf(stock.getUnitSp()),
            String.valueOf(newQuantity),
            String.valueOf(stock.getReorderLevel())
        };
        stockDao.update(stock, params);
    }

    public double getTotalStockValue() throws SQLException {
        List<Stock> allStock = stockDao.getAll();
        return allStock.stream()
                .mapToDouble(stock -> stock.getUnitBp() * stock.getQuantity())
                .sum();
    }

    public double getTotalSellingValue() throws SQLException {
        List<Stock> allStock = stockDao.getAll();
        return allStock.stream()
                .mapToDouble(stock -> stock.getUnitSp() * stock.getQuantity())
                .sum();
    }

    public List<Stock> getStockByCategory(String category) throws SQLException {
        List<Stock> allStock = stockDao.getAll();
        return allStock.stream()
                .filter(stock -> category.equalsIgnoreCase(stock.getCategory()))
                .collect(Collectors.toList());
    }

    public List<Stock> searchStock(String keyword) throws SQLException {
        List<Stock> allStock = stockDao.getAll();
        String lowerKeyword = keyword.toLowerCase();
        return allStock.stream()
                .filter(stock -> {
                    String name = stock.getProductName();
                    String id = stock.getProductID() == null ? null : String.valueOf(stock.getProductID());
                    String category = stock.getCategory();
                    return (name != null && name.toLowerCase().contains(lowerKeyword)) ||
                           (id != null && id.toLowerCase().contains(lowerKeyword)) ||
                           (category != null && category.toLowerCase().contains(lowerKeyword));
                })
                .collect(Collectors.toList());
    }

    public int getTotalItemsCount() throws SQLException {
        List<Stock> allStock = stockDao.getAll();
        return allStock.stream()
                .mapToInt(Stock::getQuantity)
                .sum();
    }

    public List<String> getAllCategories() throws SQLException {
        List<Stock> allStock = stockDao.getAll();
        return allStock.stream()
                .map(Stock::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean isStockLow(int productID) throws SQLException {
        Stock stock = getStockById(productID);
        return stock != null && stock.getQuantity() <= stock.getReorderLevel();
    }
}
