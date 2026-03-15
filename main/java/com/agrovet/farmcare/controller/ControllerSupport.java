package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.models.Users;
import jakarta.servlet.http.HttpSession;

final class ControllerSupport {
    private ControllerSupport() {}

    static Users sessionUser(HttpSession session) {
        Object user = session == null ? null : session.getAttribute("user");
        return user instanceof Users u ? u : null;
    }

    static boolean isAdminOrSuperuser(Users user) {
        if (user == null || user.getRole() == null) return false;
        String role = user.getRole().trim();
        return role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("superuser");
    }

    static String popSessionString(HttpSession session, String key) {
        Object value = session == null ? null : session.getAttribute(key);
        if (value instanceof String s) {
            session.removeAttribute(key);
            return s;
        }
        return null;
    }

    static String rootMessage(Throwable t) {
        if (t == null) return "Unknown error";
        Throwable cur = t;
        Throwable next;
        while ((next = cur.getCause()) != null && next != cur) cur = next;
        String msg = cur.getMessage();
        return msg == null || msg.isBlank() ? cur.getClass().getSimpleName() : msg;
    }
}
