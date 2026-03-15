package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.models.Stock;
import com.agrovet.farmcare.models.Users;
import com.agrovet.farmcare.service.ExpenseService;
import com.agrovet.farmcare.service.PurchaseService;
import com.agrovet.farmcare.service.SalesService;
import com.agrovet.farmcare.service.StockService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportsController {

    @Autowired
    private StockService stockService;

    @Autowired
    private SalesService salesService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private PurchaseService purchaseService;

    @GetMapping
    public String reportsPage(@RequestParam(required = false, defaultValue = "") String from,
                              @RequestParam(required = false, defaultValue = "") String to,
                              Model model,
                              HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";
        if (!ControllerSupport.isAdminOrSuperuser(user)) {
            model.addAttribute("user", user);
            return "forbidden";
        }

        model.addAttribute("user", user);

        try {
            double stockValue = stockService.getTotalStockValue();
            double sellingValue = stockService.getTotalSellingValue();
            double totalSales = salesService.getTotalSalesRevenue();
            double totalExpenses = expenseService.getTotalExpenses();
            double totalPurchases = purchaseService.getTotalPurchaseCost();

            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            LocalDate rangeFrom = parseIsoDate(from);
            LocalDate rangeTo = parseIsoDate(to);
            if (rangeFrom != null && rangeTo == null) rangeTo = rangeFrom;
            if (rangeTo != null && rangeFrom == null) rangeFrom = rangeTo;
            boolean hasRange = rangeFrom != null && rangeTo != null;

            double todayRevenue = salesService.getTotalSalesRevenueByDateRange(today, today);
            double todayCost = purchaseService.getTotalPurchaseCostByDateRange(today, today);
            double todayExpense = expenseService.getTotalExpensesByDateRange(today, today);

            double monthRevenue = salesService.getTotalSalesRevenueByDateRange(monthStart, monthEnd);
            double monthCost = purchaseService.getTotalPurchaseCostByDateRange(monthStart, monthEnd);
            double monthExpense = expenseService.getTotalExpensesByDateRange(monthStart, monthEnd);

            double rangeRevenue = hasRange ? salesService.getTotalSalesRevenueByDateRange(rangeFrom, rangeTo) : 0.0;
            double rangeCost = hasRange ? purchaseService.getTotalPurchaseCostByDateRange(rangeFrom, rangeTo) : 0.0;
            double rangeExpense = hasRange ? expenseService.getTotalExpensesByDateRange(rangeFrom, rangeTo) : 0.0;
            List<String> topProducts = salesService.getTopSellingProducts(5);
            List<Stock> lowStock = stockService.getLowStockItems();

            model.addAttribute("stockValue", stockValue);
            model.addAttribute("sellingValue", sellingValue);
            model.addAttribute("totalSales", totalSales);
            model.addAttribute("totalExpenses", totalExpenses);
            model.addAttribute("totalPurchases", totalPurchases);
            model.addAttribute("today", today.toString());
            model.addAttribute("todayRevenue", todayRevenue);
            model.addAttribute("todayCost", todayCost);
            model.addAttribute("todayExpense", todayExpense);
            model.addAttribute("monthStart", monthStart.toString());
            model.addAttribute("monthEnd", monthEnd.toString());
            model.addAttribute("monthRevenue", monthRevenue);
            model.addAttribute("monthCost", monthCost);
            model.addAttribute("monthExpense", monthExpense);
            model.addAttribute("hasRange", hasRange);
            model.addAttribute("rangeFrom", hasRange ? rangeFrom.toString() : "");
            model.addAttribute("rangeTo", hasRange ? rangeTo.toString() : "");
            model.addAttribute("rangeRevenue", rangeRevenue);
            model.addAttribute("rangeCost", rangeCost);
            model.addAttribute("rangeExpense", rangeExpense);
            model.addAttribute("topProducts", topProducts);
            model.addAttribute("lowStock", lowStock);
        } catch (Exception e) {
            model.addAttribute("error", ControllerSupport.rootMessage(e));
            model.addAttribute("topProducts", List.of());
            model.addAttribute("lowStock", List.of());
            model.addAttribute("stockValue", 0.0);
            model.addAttribute("sellingValue", 0.0);
            model.addAttribute("totalSales", 0.0);
            model.addAttribute("totalExpenses", 0.0);
            model.addAttribute("totalPurchases", 0.0);
            model.addAttribute("today", LocalDate.now().toString());
            model.addAttribute("todayRevenue", 0.0);
            model.addAttribute("todayCost", 0.0);
            model.addAttribute("todayExpense", 0.0);
            LocalDate t = LocalDate.now();
            model.addAttribute("monthStart", t.withDayOfMonth(1).toString());
            model.addAttribute("monthEnd", t.withDayOfMonth(t.lengthOfMonth()).toString());
            model.addAttribute("monthRevenue", 0.0);
            model.addAttribute("monthCost", 0.0);
            model.addAttribute("monthExpense", 0.0);
            model.addAttribute("hasRange", false);
            model.addAttribute("rangeFrom", "");
            model.addAttribute("rangeTo", "");
            model.addAttribute("rangeRevenue", 0.0);
            model.addAttribute("rangeCost", 0.0);
            model.addAttribute("rangeExpense", 0.0);
        }

        return "reports";
    }

    private static LocalDate parseIsoDate(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        return LocalDate.parse(s);
    }
}
