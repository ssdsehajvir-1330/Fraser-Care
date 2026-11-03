package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.service.AppointmentBookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentBookingService appointmentBookingService;
    private final JdbcTemplate jdbc;

    public AppointmentController(
            AppointmentBookingService appointmentBookingService,
            JdbcTemplate jdbc) {
        this.appointmentBookingService = appointmentBookingService;
        this.jdbc = jdbc;
    }

    @PostMapping
    public ResponseEntity<?> bookAppointment(
            @RequestBody Appointment appointment,
            Authentication authentication) {

        if (authentication == null ||
            authentication instanceof AnonymousAuthenticationToken ||
            !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Please log in before booking an appointment.");
        }

        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_CUSTOMER"));

        if (!isCustomer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only customers can book appointments.");
        }

        try {
            Long customerId = jdbc.queryForObject(
                    "SELECT user_id FROM auth_users WHERE username = ?",
                    Long.class,
                    authentication.getName()
            );

            appointment.setCustomerId(customerId);

            Appointment bookedAppointment =
                    appointmentBookingService.bookAppointment(appointment);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(bookedAppointment);

        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Customer account not found.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/availability")
    public ResponseEntity<?> getAvailability(
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {

        try {
            return ResponseEntity.ok(
                    appointmentBookingService.getAvailableStartTimes(serviceId, date)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
