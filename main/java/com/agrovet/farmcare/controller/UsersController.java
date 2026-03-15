package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.models.Users;
import com.agrovet.farmcare.service.UserService;
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
@RequestMapping("/users")
public class UsersController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String usersPage(Model model, HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";
        if (!ControllerSupport.isAdminOrSuperuser(user)) {
            model.addAttribute("user", user);
            return "forbidden";
        }

        model.addAttribute("user", user);

        String message = ControllerSupport.popSessionString(session, "usersMessage");
        String error = ControllerSupport.popSessionString(session, "usersError");
        if (message != null) model.addAttribute("message", message);
        if (error != null) model.addAttribute("error", error);

        try {
            List<Users> users = userService.getAllUsers();
            model.addAttribute("users", users);
        } catch (Exception e) {
            model.addAttribute("users", List.of());
            model.addAttribute("error", ControllerSupport.rootMessage(e));
        }

        return "users";
    }

    @PostMapping("/add")
    public String addUser(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false, defaultValue = "") String email,
                          @RequestParam(required = false, defaultValue = "") String firstName,
                          @RequestParam(required = false, defaultValue = "") String lastName,
                          @RequestParam(required = false, defaultValue = "") String idNumber,
                          @RequestParam(required = false, defaultValue = "user") String role,
                          HttpSession session,
                          Model model) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";
        if (!ControllerSupport.isAdminOrSuperuser(user)) {
            model.addAttribute("user", user);
            return "forbidden";
        }

        try {
            Users newUser = new Users();
            newUser.setUsername(username == null ? "" : username.trim());
            newUser.setPassword(password == null ? "" : password);
            newUser.setEmail(email == null ? "" : email.trim());
            newUser.setFirstName(firstName == null ? "" : firstName.trim());
            newUser.setLastName(lastName == null ? "" : lastName.trim());
            newUser.setIDNumber(idNumber == null ? "" : idNumber.trim());
            newUser.setRole(role == null ? "user" : role.trim());
            userService.registerUser(newUser);
            session.setAttribute("usersMessage", "User created.");
        } catch (Exception e) {
            session.setAttribute("usersError", "Add failed: " + ControllerSupport.rootMessage(e));
        }

        return "redirect:/users";
    }

    @PostMapping("/delete")
    public String deleteUser(@RequestParam String username, HttpSession session, Model model) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) return "redirect:/login";
        if (!ControllerSupport.isAdminOrSuperuser(user)) {
            model.addAttribute("user", user);
            return "forbidden";
        }

        try {
            String target = username == null ? "" : username.trim();
            if (target.equalsIgnoreCase(user.getUsername())) {
                throw new IllegalArgumentException("You cannot delete your own account while signed in");
            }
            userService.deleteUser(target);
            session.setAttribute("usersMessage", "User deleted.");
        } catch (Exception e) {
            session.setAttribute("usersError", "Delete failed: " + ControllerSupport.rootMessage(e));
        }

        return "redirect:/users";
    }
}
