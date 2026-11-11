package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.service.WaitlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final JdbcTemplate jdbc;

    public WaitlistController(
            WaitlistService waitlistService,
            JdbcTemplate jdbc) {

        this.waitlistService = waitlistService;
        this.jdbc = jdbc;
    }

    // Provide CSRF token to the authenticated frontend
    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(HttpServletRequest request) {

        CsrfToken token = (CsrfToken) request.getAttribute(
            CsrfToken.class.getName()
        );

        if (token == null) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("CSRF token unavailable");
        }

        return ResponseEntity.ok(Map.of(
            "headerName", token.getHeaderName(),
            "token", token.getToken()
        ));
    }

    // Customer joins a waitlist
    @PostMapping
    public ResponseEntity<?> joinWaitlist(
            @RequestBody WaitlistEntry entry,
            Authentication authentication) {

        // Require a logged-in customer
        if (authentication == null ||
            authentication.getAuthorities().stream()
                .noneMatch(authority ->
                    "ROLE_CUSTOMER".equals(authority.getAuthority()))) {

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Customer login required.");
        }

        if (entry == null) {
            return ResponseEntity
                .badRequest()
                .body("Missing required waitlist information.");
        }

        // Get customer ID from the authenticated account
        Long customerId;

        try {
            customerId = jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                authentication.getName()
            );
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Customer account not found.");
        }

        if (customerId == null) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Customer ID unavailable.");
        }

        // Never trust a customer ID provided by the browser
        entry.setCustomerId(customerId);

        try {
            WaitlistEntry savedEntry =
                waitlistService.joinWaitlist(entry);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedEntry);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(e.getMessage());
        }
    }

    // Retrieve waitlists belonging to the logged-in customer
    @GetMapping("/my")
    public ResponseEntity<?> getMyWaitlists(
            Authentication authentication) {

        // Require a logged-in customer
        if (authentication == null ||
            !authentication.isAuthenticated()) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Please log in to view your waitlists.");
        }

        boolean isCustomer = authentication.getAuthorities().stream()
            .anyMatch(authority ->
                "ROLE_CUSTOMER".equals(authority.getAuthority()));

        if (!isCustomer) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Only customers can view their waitlists.");
        }

        // Retrieve customer ID using authenticated username
        Long customerId;

        try {
            customerId = jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                authentication.getName()
            );

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Customer account not found.");
        }

        if (customerId == null) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Customer ID unavailable.");
        }

        // Retrieve all waitlist entries for this customer
        List<WaitlistEntry> entries =
            waitlistService.getCustomerWaitlists(customerId);

        return ResponseEntity.ok(entries);
    }
}
