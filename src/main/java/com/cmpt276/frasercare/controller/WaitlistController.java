package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.service.WaitlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
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
    public ResponseEntity<?> joinWaitlist(@RequestBody WaitlistEntry entry) {

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
}
