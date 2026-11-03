package com.cmpt276.frasercare.controller;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Admin-only list of every account and its roles.
 * Access is limited to ADMIN in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final JdbcTemplate jdbc;

    public AdminUserController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> getAllUsers() {
        return jdbc.query("""
            SELECT u.user_id, u.full_name, u.username,
                   COALESCE(STRING_AGG(a.authority, ',' ORDER BY a.authority), '') AS roles
            FROM auth_users u
            LEFT JOIN auth_authorities a ON a.username = u.username
            GROUP BY u.user_id, u.full_name, u.username
            ORDER BY u.user_id
            """,
            (rs, rowNum) -> {
                Map<String, Object> user = new LinkedHashMap<>();

                user.put("userId", rs.getLong("user_id"));
                user.put("fullName", rs.getString("full_name"));
                user.put("email", rs.getString("username"));

                // "ROLE_ADMIN,ROLE_STAFF" -> ["ADMIN", "STAFF"]
                List<String> roles = Arrays.stream(rs.getString("roles").split(","))
                    .filter(role -> !role.isBlank())
                    .map(role -> role.replaceFirst("^ROLE_", ""))
                    .toList();

                user.put("roles", roles);

                return user;
            });
    }
}
