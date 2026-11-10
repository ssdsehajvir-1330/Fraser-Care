package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.service.PickupService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pickups")
public class PickupController {

    private final PickupService pickupService;
    private final JdbcTemplate jdbc;

    public PickupController(PickupService pickupService, JdbcTemplate jdbc) {
        this.pickupService = pickupService;
        this.jdbc = jdbc;
    }

    public record PickupForm(String itemName, String notes) {
    }

    // Customer submits a pickup request
    @PostMapping
    public ResponseEntity<?> requestPickup(
            @RequestBody PickupForm form,
            Authentication authentication) {

        Long customerId = findCustomerId(authentication);

        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Customer account not found.");
        }

        if (form == null) {
            return ResponseEntity.badRequest().body("Missing pickup details.");
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(pickupService.createRequest(
                            customerId, form.itemName(), form.notes()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Customer sees only their own pickups
    @GetMapping("/mine")
    public ResponseEntity<?> myPickups(Authentication authentication) {

        Long customerId = findCustomerId(authentication);

        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Customer account not found.");
        }

        return ResponseEntity.ok(pickupService.getCustomerPickups(customerId));
    }

    // Customer ID comes from the login and not the browser
    private Long findCustomerId(Authentication authentication) {

        if (authentication == null) {
            return null;
        }

        try {
            return jdbc.queryForObject(
                    "SELECT user_id FROM auth_users WHERE username = ?",
                    Long.class,
                    authentication.getName());

        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}