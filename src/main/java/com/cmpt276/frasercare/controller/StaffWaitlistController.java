package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.PharmacyServiceRepository;
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

    public StaffWaitlistController(
            StaffWaitlistService staffWaitlistService,
            CustomerNameService customerNameService,
            PharmacyServiceRepository pharmacyServiceRepository) {

        this.staffWaitlistService = staffWaitlistService;
        this.customerNameService = customerNameService;
        this.pharmacyServiceRepository = pharmacyServiceRepository;
    }

    // Retrieve waitlist entries for a selected date
    @GetMapping
    public ResponseEntity<List<StaffWaitlistResponse>> getWaitlists(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        List<WaitlistEntry> entries =
            staffWaitlistService.getWaitlistsForDate(date);

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

        // Build staff facing responses
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
                entry.getQueuePosition()
            ));
        }

        return ResponseEntity.ok(responses);
    }

    // Notify the first customer for an available cancelled slot
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

        boolean isStaff = authentication.getAuthorities().stream()
            .anyMatch(authority ->
                "ROLE_STAFF".equals(authority.getAuthority())
                || "ROLE_ADMIN".equals(authority.getAuthority())
            );

        if (!isStaff) {
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

    // Data returned to the staff dashboard
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
        Integer queuePosition
    ) {
    }
}
