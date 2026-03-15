package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.models.Purchases;
import com.agrovet.farmcare.models.Users;
import com.agrovet.farmcare.service.PurchaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @GetMapping
    public String purchasesPage(Model model, HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);

        String message = ControllerSupport.popSessionString(session, "purchaseMessage");
        String error = ControllerSupport.popSessionString(session, "purchaseError");
        if (message != null) model.addAttribute("message", message);
        if (error != null) model.addAttribute("error", error);

        try {
            List<Purchases> purchases = ControllerSupport.isAdminOrSuperuser(user)
                    ? purchaseService.getAllPurchases()
                    : purchaseService.getPurchasesByUser(user.getUsername());
            model.addAttribute("purchases", purchases);
        } catch (Exception e) {
            model.addAttribute("purchases", List.of());
            model.addAttribute("error", ControllerSupport.rootMessage(e));
        }

        return "purchases";
    }

    @PostMapping("/add")
    public String addPurchase(@RequestParam String productName,
                              @RequestParam int quantity,
                              @RequestParam double buyingPrice,
                              @RequestParam String supplierName,
                              @RequestParam(required = false, defaultValue = "") String purchaseDate,
                              HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        try {
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than 0");
            if (buyingPrice < 0) throw new IllegalArgumentException("Buying price must be 0 or greater");
            Purchases p = new Purchases();
            p.setUsername(user.getUsername());
            p.setProductName(productName == null ? "" : productName.trim());
            p.setQuantity(quantity);
            p.setBuyingPrice(buyingPrice);
            p.setSupplierName(supplierName == null ? "" : supplierName.trim());
            String rawDate = purchaseDate == null ? "" : purchaseDate.trim();
            p.setPurchaseDate(rawDate.isEmpty() ? new java.util.Date() : java.sql.Date.valueOf(rawDate));
            // Only admin/superuser can create a brand-new stock item through purchases.
            purchaseService.recordPurchase(p, ControllerSupport.isAdminOrSuperuser(user));
            session.setAttribute("purchaseMessage", "Purchase recorded.");
        } catch (Exception e) {
            session.setAttribute("purchaseError", "Add failed: " + ControllerSupport.rootMessage(e));
        }

        return "redirect:/purchases";
    }

    @PostMapping("/delete")
    public String deletePurchase(@RequestParam int purchaseID, HttpSession session, Model model) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        try {
            if (!ControllerSupport.isAdminOrSuperuser(user)) {
                Purchases purchase = purchaseService.getPurchaseById(purchaseID);
                if (purchase == null || !user.getUsername().equals(purchase.getUsername())) {
                    model.addAttribute("user", user);
                    return "forbidden";
                }
            }
            purchaseService.deletePurchase(purchaseID);
            session.setAttribute("purchaseMessage", "Purchase deleted.");
        } catch (Exception e) {
            session.setAttribute("purchaseError", "Delete failed: " + ControllerSupport.rootMessage(e));
        }

        return "redirect:/purchases";
    }
}
