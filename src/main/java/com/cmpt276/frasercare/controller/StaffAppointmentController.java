package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.dto.StaffAppointmentResponse;
import com.cmpt276.frasercare.service.AppointmentBookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/staff/appointments")
public class StaffAppointmentController {

    private final AppointmentBookingService appointmentBookingService;

    public StaffAppointmentController(
            AppointmentBookingService appointmentBookingService) {
        this.appointmentBookingService = appointmentBookingService;
    }

    @GetMapping
    public ResponseEntity<List<StaffAppointmentResponse>> getAppointmentsByDate(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        List<StaffAppointmentResponse> appointments =
                appointmentBookingService.getStaffAppointmentsByDate(date);

        return ResponseEntity.ok(appointments);
    }
}
