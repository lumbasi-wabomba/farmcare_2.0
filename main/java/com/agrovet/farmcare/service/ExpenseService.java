package com.agrovet.farmcare.service;

import com.agrovet.farmcare.dao.ExpenseDao;
import com.agrovet.farmcare.models.Expense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {
    @Autowired
    private ExpenseDao expenseDao;

    public Expense recordExpense(Expense expense) throws SQLException {
        // Set current date if not provided
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(new Date());
        }
        return expenseDao.save(expense);
    }

    public Expense getExpenseById(int expenseID) throws SQLException {
        Expense expense = new Expense();
        expense.setExpenseID(expenseID);
        return expenseDao.get(expense);
    }

    public List<Expense> getAllExpenses() throws SQLException {
        return expenseDao.getAll();
    }

    public Expense updateExpense(Expense expense, String[] params) throws SQLException {
        return expenseDao.update(expense, params);
    }

    public void deleteExpense(int expenseID) throws SQLException {
        expenseDao.delete(String.valueOf(expenseID));
    }

    // Expense tracking and reporting features
    public List<Expense> getExpensesByUser(String username) throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        return allExpenses.stream()
                .filter(expense -> username.equals(expense.getUsername()))
                .collect(Collectors.toList());
    }

    public List<Expense> getExpensesByType(String expenseType) throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        return allExpenses.stream()
                .filter(expense -> expenseType.equalsIgnoreCase(expense.getExpenseType()))
                .collect(Collectors.toList());
    }

    public List<Expense> getExpensesByDateRange(Date startDate, Date endDate) throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        return allExpenses.stream()
                .filter(expense -> !expense.getExpenseDate().before(startDate) && !expense.getExpenseDate().after(endDate))
                .collect(Collectors.toList());
    }

    public double getTotalExpenses() throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        return allExpenses.stream()
                .mapToDouble(Expense::getTotal)
                .sum();
    }

    public double getTotalExpensesByUser(String username) throws SQLException {
        List<Expense> userExpenses = getExpensesByUser(username);
        return userExpenses.stream()
                .mapToDouble(Expense::getTotal)
                .sum();
    }

    public double getTotalExpensesByType(String expenseType) throws SQLException {
        List<Expense> typeExpenses = getExpensesByType(expenseType);
        return typeExpenses.stream()
                .mapToDouble(Expense::getTotal)
                .sum();
    }

    public double getTotalExpensesByDateRange(Date startDate, Date endDate) throws SQLException {
        List<Expense> rangeExpenses = getExpensesByDateRange(startDate, endDate);
        return rangeExpenses.stream()
                .mapToDouble(Expense::getTotal)
                .sum();
    }

    public double getTotalExpensesByDateRange(LocalDate from, LocalDate to) throws SQLException {
        if (from == null || to == null) return 0.0;
        LocalDate start = from.isAfter(to) ? to : from;
        LocalDate end = from.isAfter(to) ? from : to;
        return getTotalExpensesByDateRange(java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));
    }

    public double getTotalExpensesByUserAndDateRange(String username, LocalDate from, LocalDate to) throws SQLException {
        if (username == null) {
            return getTotalExpensesByDateRange(from, to);
        }
        if (from == null || to == null) return 0.0;
        LocalDate start = from.isAfter(to) ? to : from;
        LocalDate end = from.isAfter(to) ? from : to;

        List<Expense> allExpenses = expenseDao.getAll();
        return allExpenses.stream()
                .filter(e -> username.equals(e.getUsername()))
                .filter(e -> withinRange(e.getExpenseDate(), start, end))
                .mapToDouble(Expense::getTotal)
                .sum();
    }

    public Map<String, Double> getExpensesByTypeSummary() throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        Map<String, Double> typeSummary = new HashMap<>();
        for (Expense expense : allExpenses) {
            typeSummary.put(expense.getExpenseType(),
                typeSummary.getOrDefault(expense.getExpenseType(), 0.0) + expense.getTotal());
        }
        return typeSummary;
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

    public Map<String, Double> getExpensesByUserSummary() throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        Map<String, Double> userSummary = new HashMap<>();
        for (Expense expense : allExpenses) {
            userSummary.put(expense.getUsername(),
                userSummary.getOrDefault(expense.getUsername(), 0.0) + expense.getTotal());
        }
        return userSummary;
    }

    public List<String> getAllExpenseTypes() throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        return allExpenses.stream()
                .map(Expense::getExpenseType)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Expense> getRecentExpenses(int limit) throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        return allExpenses.stream()
                .sorted((e1, e2) -> e2.getExpenseDate().compareTo(e1.getExpenseDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Expense> getHighValueExpenses(double threshold) throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        return allExpenses.stream()
                .filter(expense -> expense.getTotal() >= threshold)
                .collect(Collectors.toList());
    }

    // Monthly expense report
    public Map<String, Double> getMonthlyExpenseReport(int year) throws SQLException {
        List<Expense> allExpenses = expenseDao.getAll();
        Map<String, Double> monthlyReport = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        for (Expense expense : allExpenses) {
            cal.setTime(expense.getExpenseDate());
            if (cal.get(Calendar.YEAR) == year) {
                String monthKey = String.format("%02d", cal.get(Calendar.MONTH) + 1);
                monthlyReport.put(monthKey, monthlyReport.getOrDefault(monthKey, 0.0) + expense.getTotal());
            }
        }
        return monthlyReport;
    }

    // Bulk expense recording
    public List<Expense> recordBulkExpenses(List<Expense> expenses) throws SQLException {
        List<Expense> recordedExpenses = new ArrayList<>();
        for (Expense expense : expenses) {
            Expense recorded = recordExpense(expense);
            recordedExpenses.add(recorded);
        }
        return recordedExpenses;
    }
}
