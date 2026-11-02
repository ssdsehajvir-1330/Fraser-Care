package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.service.WaitlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
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
