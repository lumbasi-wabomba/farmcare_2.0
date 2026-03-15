package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.models.Stock;
import com.agrovet.farmcare.models.StockPage;
import com.agrovet.farmcare.models.StockSort;
import com.agrovet.farmcare.models.Users;
import com.agrovet.farmcare.service.StockService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.SQLException;

@Controller
@RequestMapping("/stock")
public class StockController {

    private static final int PAGE_SIZE = 25;

    @Autowired
    private StockService stockService;

    @GetMapping
    public String stockPage(@RequestParam(name = "page", defaultValue = "1") int page,
                            @RequestParam(name = "sort", defaultValue = "quantity") String sort,
                            Model model,
                            HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (!ControllerSupport.isAdminOrSuperuser(user)) {
            model.addAttribute("user", user);
            return "forbidden";
        }

        String message = ControllerSupport.popSessionString(session, "stockMessage");
        String error = ControllerSupport.popSessionString(session, "stockError");
        if (message != null) model.addAttribute("message", message);
        if (error != null) model.addAttribute("error", error);

        StockSort stockSort = StockSort.fromParam(sort);
        int safePage = Math.max(page, 1);
        try {
            StockPage stockPage = stockService.getStockPage(safePage, PAGE_SIZE, stockSort);
            model.addAttribute("user", user);
            model.addAttribute("stocks", stockPage.items());
            model.addAttribute("page", stockPage.page());
            model.addAttribute("pageSize", stockPage.pageSize());
            model.addAttribute("totalItems", stockPage.totalItems());
            model.addAttribute("totalPages", stockPage.totalPages());
            model.addAttribute("sort", stockPage.sort().param());
            return "stock";
        } catch (SQLException e) {
            model.addAttribute("user", user);
            model.addAttribute("error", ControllerSupport.rootMessage(e));
            model.addAttribute("stocks", java.util.List.of());
            model.addAttribute("page", 1);
            model.addAttribute("pageSize", PAGE_SIZE);
            model.addAttribute("totalItems", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("sort", stockSort.param());
            return "stock";
        }
    }

    @PostMapping("/add")
    public String addStock(@RequestParam String productName,
                           @RequestParam(required = false, defaultValue = "") String productDescription,
                           @RequestParam(required = false, defaultValue = "") String category,
                           @RequestParam double unitBp,
                           @RequestParam double unitSp,
                           @RequestParam int quantity,
                           @RequestParam int reorderLevel,
                           HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";
        if (!ControllerSupport.isAdminOrSuperuser(user)) return "forbidden";

        try {
            Stock stock = new Stock(null, productName, productDescription, category, unitBp, unitSp, quantity, reorderLevel);
            Stock saved = stockService.addStock(stock);
            session.setAttribute("stockMessage", "Stock item added (ID: " + saved.getProductID() + ").");
        } catch (SQLException | RuntimeException e) {
            session.setAttribute("stockError", "Add failed: " + ControllerSupport.rootMessage(e));
        }
        return "redirect:/stock";
    }

    @PostMapping("/update")
    public String updateStock(@RequestParam int productID,
                              @RequestParam String productName,
                              @RequestParam(required = false, defaultValue = "") String productDescription,
                              @RequestParam(required = false, defaultValue = "") String category,
                              @RequestParam double unitBp,
                              @RequestParam double unitSp,
                              @RequestParam int quantity,
                              @RequestParam int reorderLevel,
                              HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";
        if (!ControllerSupport.isAdminOrSuperuser(user)) return "forbidden";

        try {
            Stock stock = new Stock();
            stock.setProductID(productID);
            String[] params = {
                    productName,
                    productDescription,
                    category,
                    String.valueOf(unitBp),
                    String.valueOf(unitSp),
                    String.valueOf(quantity),
                    String.valueOf(reorderLevel)
            };
            stockService.updateStock(stock, params);
            session.setAttribute("stockMessage", "Stock item updated.");
        } catch (SQLException | RuntimeException e) {
            session.setAttribute("stockError", "Update failed: " + ControllerSupport.rootMessage(e));
        }
        return "redirect:/stock";
    }

    @PostMapping("/delete")
    public String deleteStock(@RequestParam int productID, HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";
        if (!ControllerSupport.isAdminOrSuperuser(user)) return "forbidden";

        try {
            stockService.deleteStock(productID);
            session.setAttribute("stockMessage", "Stock item deleted.");
        } catch (SQLException | RuntimeException e) {
            session.setAttribute("stockError", "Delete failed: " + ControllerSupport.rootMessage(e));
        }
        return "redirect:/stock";
    }

    // ControllerSupport holds the shared auth/session helpers.
}
