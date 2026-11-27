package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.PharmacyServiceRepository;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import com.cmpt276.frasercare.service.CustomerNameService;
import com.cmpt276.frasercare.service.StaffWaitlistService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/waitlists")
public class StaffWaitlistController {

    private final StaffWaitlistService staffWaitlistService;
    private final CustomerNameService customerNameService;
    private final PharmacyServiceRepository pharmacyServiceRepository;
    private final WaitlistRepository waitlistRepository;

    public StaffWaitlistController(
            StaffWaitlistService staffWaitlistService,
            CustomerNameService customerNameService,
            PharmacyServiceRepository pharmacyServiceRepository,
            WaitlistRepository waitlistRepository) {

        this.staffWaitlistService = staffWaitlistService;
        this.customerNameService = customerNameService;
        this.pharmacyServiceRepository = pharmacyServiceRepository;
        this.waitlistRepository = waitlistRepository;
    }

    // Check if user is staff or admin
    private boolean isStaff(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                    "ROLE_STAFF".equals(authority.getAuthority())
                    || "ROLE_ADMIN".equals(authority.getAuthority())
                );
    }

    // Retrieve upcoming dates containing active waitlists
    @GetMapping("/active-dates")
    public ResponseEntity<?> getActiveWaitlistDates(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Please log in as staff.");
        }

        if (!isStaff(authentication)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Staff access required.");
        }

        LocalDate today = LocalDate.now(
            ZoneId.of("America/Vancouver")
        );

        return ResponseEntity.ok(
            waitlistRepository.findActiveWaitlistDates(today)
        );
    }

    // Retrieve waitlist entries for a selected date
    @GetMapping
    public ResponseEntity<List<StaffWaitlistResponse>> getWaitlists(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        List<WaitlistEntry> entries =
            staffWaitlistService.getWaitlistsForDate(date);

        // Determine customers ready for notification
        Map<Long, StaffWaitlistService.NotificationEligibility> eligibility =
            staffWaitlistService.getNotificationEligibilityForDate(date);

        // Retrieve customer names
        Set<Long> customerIds = entries.stream()
            .map(WaitlistEntry::getCustomerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> customerNames =
            customerNameService.getNames(customerIds);

        // Retrieve service names
        Set<Long> serviceIds = entries.stream()
            .map(WaitlistEntry::getServiceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> serviceNames = new HashMap<>();

        for (PharmacyService service :
                pharmacyServiceRepository.findAllById(serviceIds)) {

            serviceNames.put(
                service.getServiceId(),
                service.getName()
            );
        }

        // Build staff-facing responses
        List<StaffWaitlistResponse> responses =
            new ArrayList<>();

        for (WaitlistEntry entry : entries) {

            String customerName = customerNames.getOrDefault(
                entry.getCustomerId(),
                "Customer #" + entry.getCustomerId()
            );

            String serviceName = serviceNames.getOrDefault(
                entry.getServiceId(),
                "Service #" + entry.getServiceId()
            );

            StaffWaitlistService.NotificationEligibility decision =
                eligibility.get(entry.getWaitlistId());

            boolean readyToNotify =
                decision != null && decision.ready();

            String notificationReason =
                decision != null
                    ? decision.reason()
                    : "Eligibility unavailable.";

            responses.add(new StaffWaitlistResponse(
                entry.getWaitlistId(),
                entry.getCustomerId(),
                customerName,
                entry.getServiceId(),
                serviceName,
                entry.getAppointmentDate(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getJoinedAt(),
                entry.getStatus(),
                entry.getQueuePosition(),
                readyToNotify,
                notificationReason,
                entry.getSkipReason()
            ));
        }

        return ResponseEntity.ok(responses);
    }

    // Notify 1st eligible customer
    @PostMapping("/{waitlistId}/notify")
    public ResponseEntity<?> notifyNextCustomer(
            @PathVariable Long waitlistId,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Please log in as staff.");
        }

        if (!isStaff(authentication)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Staff access required.");
        }

        try {
            WaitlistEntry notified =
                staffWaitlistService.notifyNextCustomer(waitlistId);

            return ResponseEntity.ok(notified);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(e.getMessage());
        }
    }

    // Staff can skip customer given a reason
    @PostMapping("/{waitlistId}/skip")
    public ResponseEntity<?> skipWaitlistEntry(
            @PathVariable Long waitlistId,
            @RequestBody SkipRequest request,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Please log in as staff.");
        }

        if (!isStaff(authentication)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Staff access required.");
        }

        if (request == null || request.reason() == null) {
            return ResponseEntity
                .badRequest()
                .body("A valid skip reason is required.");
        }

        try {
            WaitlistEntry skipped =
                staffWaitlistService.skipWaitlistEntry(
                    waitlistId,
                    request.reason()
                );

            return ResponseEntity.ok(skipped);

        } catch (IllegalArgumentException e) {

            // Missing entries return 404, invalid return 400
            if ("Waitlist entry not found.".equals(e.getMessage())) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
            }

            return ResponseEntity
                .badRequest()
                .body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(e.getMessage());
        }
    }

    // JSON request containing staff selected skip reason
    public record SkipRequest(
        String reason
    ) {
    }

    // Data returned to staff dashboard
    public record StaffWaitlistResponse(
        Long waitlistId,
        Long customerId,
        String customerName,
        Long serviceId,
        String serviceName,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        LocalDateTime joinedAt,
        String status,
        Integer queuePosition,
        boolean readyToNotify,
        String notificationReason,
        String skipReason
    ) {
    }
}