package com.cmpt276.frasercare;

import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import java.util.Map;

@RestController
public class AuthController {

    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    public AuthController(
            JdbcUserDetailsManager users,
            PasswordEncoder encoder,
            JdbcTemplate jdbc) {
        this.users = users;
        this.encoder = encoder;
        this.jdbc = jdbc;
    }

    public record Registration(
        String fullName,
        String email,
        String password
    ) {}

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of(
            "token", token.getToken(),
            "headerName", token.getHeaderName()
        );
    }

    // Returns the logged-in user, or 401 if nobody is logged in
    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> currentUser(
            Authentication authentication) {

        if (authentication == null ||
            authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();

        Map<String, Object> user;

        try {
            user = jdbc.queryForMap(
                "SELECT user_id, full_name FROM auth_users WHERE username = ?",
                email
            );
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        Object fullName = user.get("full_name");

        return ResponseEntity.ok(Map.of(
            "userId", user.get("user_id"),
            "email", email,
            "fullName", fullName == null ? "" : fullName,
            "roles", roles
        ));
    }

    @PostMapping("/api/register")
    @Transactional
    public ResponseEntity<String> register(
            @RequestBody Registration request) {

        if (request == null ||
            request.fullName() == null ||
            request.email() == null ||
            request.password() == null) {
            return ResponseEntity.badRequest()
                .body("All fields are required.");
        }

        String fullName = request.fullName().trim();
        String email = request.email().trim()
            .toLowerCase(Locale.ROOT);

        if (fullName.isBlank() || fullName.length() > 255 ||
            email.length() > 255 ||
            !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ||
            request.password().length() < 8 ||
            request.password().length() > 72) {
            return ResponseEntity.badRequest()
                .body("Enter a valid name, email, and password (8–72 characters).");
        }

        if (users.userExists(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("This email is already registered.");
        }

        UserDetails customer = User.withUsername(email)
            .password(encoder.encode(request.password()))
            .roles("CUSTOMER")
            .build();

        try {
            users.createUser(customer);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("This email is already registered.");
        }

        jdbc.update(
            "UPDATE auth_users SET full_name = ? WHERE username = ?",
            fullName, email
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body("Account created successfully!");
    }
}
