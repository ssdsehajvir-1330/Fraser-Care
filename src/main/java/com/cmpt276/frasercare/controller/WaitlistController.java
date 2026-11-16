package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.service.WaitlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
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

    // Check whether the logged-in user is a customer
    private boolean isCustomer(Authentication authentication) {

        return authentication != null
            && authentication.isAuthenticated()
            && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                    "ROLE_CUSTOMER".equals(
                        authority.getAuthority()
                    )
                );
    }

    // Retrieve customer ID from the authenticated account
    private Long getAuthenticatedCustomerId(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return null;
        }

        try {
            return jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                authentication.getName()
            );

        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // Provide CSRF token to the frontend
    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(
            HttpServletRequest request) {

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

        if (!isCustomer(authentication)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Customer login required.");
        }

        if (entry == null) {
            return ResponseEntity
                .badRequest()
                .body("Missing required waitlist information.");
        }

        Long customerId = getAuthenticatedCustomerId(
            authentication
        );

        if (customerId == null) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Customer account not found.");
        }

        // Always use the authenticated customer ID
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

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Please log in to view your waitlists.");
        }

        if (!isCustomer(authentication)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Only customers can view their waitlists.");
        }

        Long customerId = getAuthenticatedCustomerId(
            authentication
        );

        if (customerId == null) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Customer account not found.");
        }

        List<WaitlistEntry> entries =
            waitlistService.getCustomerWaitlists(customerId);

        return ResponseEntity.ok(entries);
    }

    // Remove an entry belonging to the logged-in customer
    @DeleteMapping("/{waitlistId}")
    public ResponseEntity<?> leaveWaitlist(
            @PathVariable Long waitlistId,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Please log in to leave a waitlist.");
        }

        if (!isCustomer(authentication)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Only customers can leave their waitlists.");
        }

        Long customerId = getAuthenticatedCustomerId(
            authentication
        );

        if (customerId == null) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Customer account not found.");
        }

        try {
            waitlistService.leaveWaitlist(
                waitlistId,
                customerId
            );

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            // Do not reveal another customer's entries
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Waitlist entry not found.");

        } catch (IllegalStateException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(e.getMessage());
        }
    }
}
