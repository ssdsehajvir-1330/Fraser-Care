package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.dto.PickupResponse;
import com.cmpt276.frasercare.model.PickupRequest;
import com.cmpt276.frasercare.service.CustomerNameService;
import com.cmpt276.frasercare.service.PickupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/pickups")
public class StaffPickupController {

    private final PickupService pickupService;
    private final CustomerNameService customerNameService;

    public StaffPickupController(
            PickupService pickupService,
            CustomerNameService customerNameService) {
        this.pickupService = pickupService;
        this.customerNameService = customerNameService;
    }

    public record StatusUpdate(String status) {
    }

    // All pickups, optionally filtered by status
    @GetMapping
    public ResponseEntity<?> getPickups(
            @RequestParam(required = false) String status) {

        try {
            List<PickupRequest> pickups = pickupService.getAllPickups(status);

            Set<Long> customerIds = pickups.stream()
                    .map(PickupRequest::getCustomerId)
                    .collect(Collectors.toSet());

            Map<Long, String> names = customerNameService.getNames(customerIds);

            return ResponseEntity.ok(pickups.stream()
                    .map(pickup -> PickupResponse.from(
                            pickup, names.get(pickup.getCustomerId())))
                    .toList());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Move a pickup to its next status
    @PatchMapping("/{pickupId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long pickupId,
            @RequestBody StatusUpdate update) {

        try {
            PickupRequest pickup = pickupService.updateStatus(
                    pickupId, update == null ? null : update.status());

            String name = customerNameService
                    .getNames(Set.of(pickup.getCustomerId()))
                    .get(pickup.getCustomerId());

            return ResponseEntity.ok(PickupResponse.from(pickup, name));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}