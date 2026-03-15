package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.models.Sales;
import com.agrovet.farmcare.models.Users;
import com.agrovet.farmcare.service.ExpenseService;
import com.agrovet.farmcare.service.SalesService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/sales")
public class SalesController {

    @Autowired
    private SalesService salesService;

    @Autowired
    private ExpenseService expenseService;

    @GetMapping
    public String salesPage(@RequestParam(required = false, defaultValue = "") String from,
                            @RequestParam(required = false, defaultValue = "") String to,
                            Model model,
                            HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);

        String message = ControllerSupport.popSessionString(session, "salesMessage");
        String error = ControllerSupport.popSessionString(session, "salesError");
        if (message != null) model.addAttribute("message", message);
        if (error != null) model.addAttribute("error", error);

        try {
            boolean isAdmin = ControllerSupport.isAdminOrSuperuser(user);
            String usernameFilter = isAdmin ? null : user.getUsername();

            List<Sales> sales = isAdmin ? salesService.getAllSales() : salesService.getSalesByUser(user.getUsername());
            model.addAttribute("sales", sales);
            model.addAttribute("isAdminOrSuperuser", isAdmin);

            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            LocalDate rangeFrom = parseIsoDate(from);
            LocalDate rangeTo = parseIsoDate(to);
            if (rangeFrom != null && rangeTo == null) rangeTo = rangeFrom;
            if (rangeTo != null && rangeFrom == null) rangeFrom = rangeTo;
            boolean hasRange = isAdmin && rangeFrom != null && rangeTo != null;

            double todaySalesTotal = salesService.getTotalSalesRevenueByUserAndDateRange(usernameFilter, today, today);
            double todayExpenseTotal = expenseService.getTotalExpensesByUserAndDateRange(usernameFilter, today, today);

            double monthSalesTotal = isAdmin ? salesService.getTotalSalesRevenueByUserAndDateRange(usernameFilter, monthStart, monthEnd) : 0.0;
            double monthExpenseTotal = isAdmin ? expenseService.getTotalExpensesByUserAndDateRange(usernameFilter, monthStart, monthEnd) : 0.0;

            double rangeSalesTotal = hasRange ? salesService.getTotalSalesRevenueByUserAndDateRange(usernameFilter, rangeFrom, rangeTo) : 0.0;
            double rangeExpenseTotal = hasRange ? expenseService.getTotalExpensesByUserAndDateRange(usernameFilter, rangeFrom, rangeTo) : 0.0;

            model.addAttribute("today", today.toString());
            model.addAttribute("todaySalesTotal", todaySalesTotal);
            model.addAttribute("todayExpenseTotal", todayExpenseTotal);
            model.addAttribute("monthStart", monthStart.toString());
            model.addAttribute("monthEnd", monthEnd.toString());
            model.addAttribute("monthSalesTotal", monthSalesTotal);
            model.addAttribute("monthExpenseTotal", monthExpenseTotal);
            model.addAttribute("hasRange", hasRange);
            model.addAttribute("rangeFrom", hasRange ? rangeFrom.toString() : "");
            model.addAttribute("rangeTo", hasRange ? rangeTo.toString() : "");
            model.addAttribute("rangeSalesTotal", rangeSalesTotal);
            model.addAttribute("rangeExpenseTotal", rangeExpenseTotal);
        } catch (Exception e) {
            model.addAttribute("sales", List.of());
            model.addAttribute("error", ControllerSupport.rootMessage(e));
            LocalDate t = LocalDate.now();
            model.addAttribute("isAdminOrSuperuser", ControllerSupport.isAdminOrSuperuser(user));
            model.addAttribute("today", t.toString());
            model.addAttribute("todaySalesTotal", 0.0);
            model.addAttribute("todayExpenseTotal", 0.0);
            model.addAttribute("monthStart", t.withDayOfMonth(1).toString());
            model.addAttribute("monthEnd", t.withDayOfMonth(t.lengthOfMonth()).toString());
            model.addAttribute("monthSalesTotal", 0.0);
            model.addAttribute("monthExpenseTotal", 0.0);
            model.addAttribute("hasRange", false);
            model.addAttribute("rangeFrom", "");
            model.addAttribute("rangeTo", "");
            model.addAttribute("rangeSalesTotal", 0.0);
            model.addAttribute("rangeExpenseTotal", 0.0);
        }

        return "sales";
    }

    @PostMapping("/add")
    public String addSale(@RequestParam String customerName,
                          @RequestParam String productName,
                          @RequestParam int quantity,
                          @RequestParam(required = false, defaultValue = "") String saleDate,
                          HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        try {
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than 0");
            String rawDate = saleDate == null ? "" : saleDate.trim();
            java.util.Date parsedDate = rawDate.isEmpty() ? new java.util.Date() : java.sql.Date.valueOf(rawDate);
            salesService.processCustomerBill(
                    user.getUsername(),
                    customerName == null ? "" : customerName.trim(),
                    List.of(Map.of("productName", productName == null ? "" : productName.trim(), "quantity", quantity)),
                    parsedDate
            );
            session.setAttribute("salesMessage", "Sale recorded.");
        } catch (SQLException | RuntimeException e) {
            session.setAttribute("salesError", "Add failed: " + ControllerSupport.rootMessage(e));
        }

        return "redirect:/sales";
    }

    @PostMapping("/delete")
    public String deleteSale(@RequestParam int salesID, HttpSession session, Model model) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        try {
            if (!ControllerSupport.isAdminOrSuperuser(user)) {
                Sales sale = salesService.getSaleById(salesID);
                if (sale == null || !user.getUsername().equals(sale.getUsername())) {
                    model.addAttribute("user", user);
                    return "forbidden";
                }
            }
            salesService.deleteSale(salesID);
            session.setAttribute("salesMessage", "Sale deleted.");
        } catch (SQLException | RuntimeException e) {
            session.setAttribute("salesError", "Delete failed: " + ControllerSupport.rootMessage(e));
        }

        return "redirect:/sales";
    }

    private static LocalDate parseIsoDate(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        return LocalDate.parse(s);
    }
}
