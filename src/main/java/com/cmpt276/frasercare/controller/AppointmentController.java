package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.service.AppointmentBookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentBookingService appointmentBookingService;

    public AppointmentController(AppointmentBookingService appointmentBookingService) {
        this.appointmentBookingService = appointmentBookingService;
    }

    @PostMapping
    public ResponseEntity<?> bookAppointment(@RequestBody Appointment appointment) {

        try {
            Appointment bookedAppointment =
                    appointmentBookingService.bookAppointment(appointment);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(bookedAppointment);

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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            return ResponseEntity.ok(
                    appointmentBookingService.getAvailableStartTimes(serviceId, date)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
