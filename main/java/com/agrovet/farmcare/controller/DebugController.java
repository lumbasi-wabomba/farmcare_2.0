package com.agrovet.farmcare.controller;

import com.agrovet.farmcare.dao.DatabaseConnection;
import com.agrovet.farmcare.models.Users;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/db")
    public Map<String, Object> db(HttpSession session) {
        Users user = ControllerSupport.sessionUser(session);
        if (user == null) {
            return Map.of("ok", false, "error", "Not logged in");
        }
        if (!ControllerSupport.isAdminOrSuperuser(user)) {
            return Map.of("ok", false, "error", "Forbidden");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);

        try (Connection conn = DatabaseConnection.getConnection()) {
            out.put("url", conn.getMetaData().getURL());
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                 ResultSet rs = stmt.executeQuery()) {
                out.put("ping", rs.next() ? rs.getInt(1) : null);
            }
            out.put("ok", true);
            return out;
        } catch (Exception e) {
            out.put("error", rootMessage(e));
            return out;
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        Throwable next;
        while ((next = cur.getCause()) != null && next != cur) cur = next;
        String msg = cur.getMessage();
        return msg == null || msg.isBlank() ? cur.getClass().getSimpleName() : msg;
    }
}

