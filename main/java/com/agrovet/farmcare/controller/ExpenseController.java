package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.models.Expense;
import com.agrovet.farmcare.models.Users;
import com.agrovet.farmcare.service.ExpenseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @GetMapping
    public String expensesPage(Model model, HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);

        String message = ControllerSupport.popSessionString(session, "expenseMessage");
        String error = ControllerSupport.popSessionString(session, "expenseError");
        if (message != null) model.addAttribute("message", message);
        if (error != null) model.addAttribute("error", error);

        try {
            List<Expense> expenses = ControllerSupport.isAdminOrSuperuser(user)
                    ? expenseService.getAllExpenses()
                    : expenseService.getExpensesByUser(user.getUsername());
            model.addAttribute("expenses", expenses);
        } catch (Exception e) {
            model.addAttribute("expenses", List.of());
            model.addAttribute("error", ControllerSupport.rootMessage(e));
        }

        return "expense";
    }

    @PostMapping("/add")
    public String addExpense(@RequestParam String expenseType,
                             @RequestParam(required = false, defaultValue = "") String expenseDescription,
                             @RequestParam double total,
                             HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        try {
            if (total < 0) throw new IllegalArgumentException("Total must be 0 or greater");
            Expense expense = new Expense();
            expense.setUsername(user.getUsername());
            expense.setExpenseType(expenseType == null ? "" : expenseType.trim());
            expense.setExpenseDescription(expenseDescription == null ? "" : expenseDescription.trim());
            expense.setTotal(total);
            expense.setExpenseDate(new Date());
            expenseService.recordExpense(expense);
            session.setAttribute("expenseMessage", "Expense recorded.");
        } catch (Exception e) {
            session.setAttribute("expenseError", "Add failed: " + ControllerSupport.rootMessage(e));
        }

        return "redirect:/expenses";
    }

    @PostMapping("/delete")
    public String deleteExpense(@RequestParam int expenseID, HttpSession session, Model model) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";

        try {
            if (!ControllerSupport.isAdminOrSuperuser(user)) {
                Expense expense = expenseService.getExpenseById(expenseID);
                if (expense == null || !user.getUsername().equals(expense.getUsername())) {
                    model.addAttribute("user", user);
                    return "forbidden";
                }
            }
            expenseService.deleteExpense(expenseID);
            session.setAttribute("expenseMessage", "Expense deleted.");
        } catch (Exception e) {
            session.setAttribute("expenseError", "Delete failed: " + ControllerSupport.rootMessage(e));
        }

        return "redirect:/expenses";
    }
}
