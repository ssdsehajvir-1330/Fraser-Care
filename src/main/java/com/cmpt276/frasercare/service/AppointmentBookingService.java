package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.repository.AppointmentRepository;
import com.cmpt276.frasercare.repository.PharmacyServiceRepository;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;

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

        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
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

        long duration = Duration.between(
                appointment.getStartTime(),
                appointment.getEndTime()
        ).toMinutes();

        if (duration != service.getDurationMinutes()) {
            throw new IllegalArgumentException("Appointment duration does not match service duration.");
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
}