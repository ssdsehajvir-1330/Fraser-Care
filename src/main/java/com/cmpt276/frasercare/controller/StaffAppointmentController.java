package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.dto.StaffAppointmentResponse;
import com.cmpt276.frasercare.service.AppointmentBookingService;
import com.cmpt276.frasercare.service.CustomerNameService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/appointments")
public class StaffAppointmentController {

    private final AppointmentBookingService appointmentBookingService;
    private final CustomerNameService customerNameService;

    public StaffAppointmentController(
            AppointmentBookingService appointmentBookingService,
            CustomerNameService customerNameService) {
        this.appointmentBookingService = appointmentBookingService;
        this.customerNameService = customerNameService;
    }

    @GetMapping
    public ResponseEntity<List<StaffAppointmentResponse>> getAppointmentsByDate(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        List<StaffAppointmentResponse> appointments =
                appointmentBookingService.getStaffAppointmentsByDate(date);

        // Look up every customer's name in one query
        Set<Long> customerIds = appointments.stream()
                .map(StaffAppointmentResponse::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> names = customerNameService.getNames(customerIds);

        List<StaffAppointmentResponse> withNames = appointments.stream()
                .map(appointment -> new StaffAppointmentResponse(
                        appointment.getAppointmentId(),
                        appointment.getCustomerId(),
                        names.get(appointment.getCustomerId()),
                        appointment.getServiceId(),
                        appointment.getServiceName(),
                        appointment.getAppointmentDate(),
                        appointment.getStartTime(),
                        appointment.getEndTime(),
                        appointment.getStatus()))
                .toList();

        return ResponseEntity.ok(withNames);
    }
}
