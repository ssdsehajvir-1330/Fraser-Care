package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.dto.StaffAppointmentResponse;
import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.repository.AppointmentRepository;
import com.cmpt276.frasercare.repository.PharmacyServiceRepository;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;

@Service
public class AppointmentBookingService {

    private final AppointmentRepository appointmentRepository;
    private final PharmacyServiceRepository pharmacyServiceRepository;

    public AppointmentBookingService(
            AppointmentRepository appointmentRepository,
            PharmacyServiceRepository pharmacyServiceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.pharmacyServiceRepository = pharmacyServiceRepository;
    }

    public Appointment bookAppointment(Appointment appointment) {

        if (appointment.getCustomerId() == null ||
            appointment.getServiceId() == null ||
            appointment.getAppointmentDate() == null ||
            appointment.getStartTime() == null ||
            appointment.getEndTime() == null) {
            throw new IllegalArgumentException("Missing appointment details.");
        }
        if (appointment.getAppointmentId() != null) {
            throw new IllegalArgumentException("Appointment ID must not be provided when booking.");
        }

        if (appointment.getAppointmentDate().isBefore(LocalDate.now(ZoneId.of("America/Vancouver")))) {
            throw new IllegalArgumentException("Appointment date cannot be in the past.");
        }

        if (!appointment.getEndTime().isAfter(appointment.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        PharmacyService service = pharmacyServiceRepository.findById(appointment.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy service not found."));

        if (!service.isActive()) {
            throw new IllegalArgumentException("Pharmacy service is not active.");
        }

        Duration duration = Duration.between(
                appointment.getStartTime(),
                appointment.getEndTime()
        );

        if (!duration.equals(Duration.ofMinutes(service.getDurationMinutes()))) {
            throw new IllegalArgumentException("Appointment duration does not match service duration.");
        }

        LocalTime openingTime = LocalTime.of(9, 0);
        LocalTime closingTime = LocalTime.of(17, 0);

        if (appointment.getStartTime().isBefore(openingTime) ||
            appointment.getEndTime().isAfter(closingTime)) {
            throw new IllegalArgumentException("Appointment must be between 9 AM and 5 PM.");
        }

        if (appointment.getStartTime().getMinute() % 15 != 0 ||
            appointment.getStartTime().getSecond() != 0 ||
            appointment.getStartTime().getNano() != 0) {
            throw new IllegalArgumentException("Appointments must start every 15 minutes.");
        }

        ZoneId timeZone = ZoneId.of("America/Vancouver");

        if (appointment.getAppointmentDate().equals(LocalDate.now(timeZone)) &&
            !appointment.getStartTime().isAfter(LocalTime.now(timeZone))) {
            throw new IllegalArgumentException("Appointment time cannot be in the past.");
        }


        boolean conflict = appointmentRepository
                .existsByAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        appointment.getAppointmentDate(),
                        "BOOKED",
                        appointment.getEndTime(),
                        appointment.getStartTime()
                );

        if (conflict) {
            throw new IllegalStateException("This appointment time is already booked.");
        }

        appointment.setStatus("BOOKED");

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsByCustomerId(Long customerId) {

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID.");
        }

        return appointmentRepository
                .findByCustomerIdOrderByAppointmentDateAscStartTimeAsc(customerId);
    }

    public Appointment cancelAppointment(Long appointmentId) {

        if (appointmentId == null || appointmentId <= 0) {
            throw new IllegalArgumentException("Invalid appointment ID.");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));

        if (!"BOOKED".equals(appointment.getStatus())) {
            throw new IllegalStateException("This appointment is not currently booked.");
        }

        appointment.setStatus("CANCELLED");

        return appointmentRepository.save(appointment);
    }
    public List<LocalTime> getAvailableStartTimes(Long serviceId, LocalDate date) {

        if (serviceId == null || serviceId <= 0 || date == null) {
            throw new IllegalArgumentException("Invalid service ID or date.");
        }

        ZoneId timeZone = ZoneId.of("America/Vancouver");
        LocalDate today = LocalDate.now(timeZone);

        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Date cannot be in the past.");
        }

        PharmacyService service = pharmacyServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy service not found."));

        if (!service.isActive()) {
            throw new IllegalArgumentException("Pharmacy service is not active.");
        }

        List<Appointment> bookedAppointments =
                appointmentRepository.findByAppointmentDateAndStatus(date, "BOOKED");

        List<LocalTime> availableTimes = new ArrayList<>();

        LocalTime openingTime = LocalTime.of(9, 0);
        LocalTime closingTime = LocalTime.of(17, 0);

        for (LocalTime start = openingTime;
             !start.plusMinutes(service.getDurationMinutes()).isAfter(closingTime);
             start = start.plusMinutes(15)) {

            LocalTime end = start.plusMinutes(service.getDurationMinutes());

            if (date.equals(today) && !start.isAfter(LocalTime.now(timeZone))) {
                continue;
            }

            boolean conflict = false;

            for (Appointment booked : bookedAppointments) {
                if (start.isBefore(booked.getEndTime()) &&
                    end.isAfter(booked.getStartTime())) {
                    conflict = true;
                    break;
                }
            }

            if (!conflict) {
                availableTimes.add(start);
            }
        }

        return availableTimes;
    }


    public List<StaffAppointmentResponse> getStaffAppointmentsByDate(LocalDate date) {

        if (date == null) {
            throw new IllegalArgumentException("Appointment date is required.");
        }

        List<Appointment> appointments =
                appointmentRepository.findByAppointmentDateOrderByStartTimeAsc(date);

        List<StaffAppointmentResponse> responses = new ArrayList<>();

        for (Appointment appointment : appointments) {

            PharmacyService service = pharmacyServiceRepository
                    .findById(appointment.getServiceId())
                    .orElse(null);

            String serviceName = service == null ? null : service.getName();

            responses.add(new StaffAppointmentResponse(
                    appointment.getAppointmentId(),
                    appointment.getCustomerId(),
                    null,
                    appointment.getServiceId(),
                    serviceName,
                    appointment.getAppointmentDate(),
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    appointment.getStatus()
            ));
        }

        return responses;
    }

}
