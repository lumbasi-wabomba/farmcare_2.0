package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.models.Users;
import com.agrovet.farmcare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;

/**
 * Web controller for handling user interface routes including landing page and authentication.
 */
@Controller
public class WebController {

    @Autowired
    private UserService userService;

    /**
     * Displays the landing page.
     * @return the name of the landing page template
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * Displays the login page.
     * @return the name of the login page template
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Handles user login authentication.
     * @param username the username entered by the user
     * @param password the password entered by the user
     * @param model the Spring MVC model for passing data to the view
     * @param session the HTTP session for storing user data
     * @return redirect to dashboard on success, or back to login on failure
     */
    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                              @RequestParam String password,
                              Model model,
                              HttpSession session) {
        try {
            Users user = userService.authenticateUser(username, password);
            if (user != null) {
                // Store user in session
                session.setAttribute("user", user);
                return "redirect:/dashboard"; // Redirect to dashboard (to be implemented)
            } else {
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }
        } catch (SQLException e) {
            model.addAttribute("error", "Database error: " + e.getMessage());
            return "login";
        }
    }

    /**
     * Handles user logout.
     * @param session the HTTP session to invalidate
     * @return redirect to home page
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /**
     * Displays the dashboard for authenticated users.
     * @param model the Spring MVC model for passing data to the view
     * @param session the HTTP session to check for authenticated user
     * @return the dashboard template or redirect to login if not authenticated
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "dashboard";
    }
}
