package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.AppointmentRepository;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StaffWaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final AppointmentRepository appointmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public StaffWaitlistService(
            WaitlistRepository waitlistRepository,
            AppointmentRepository appointmentRepository) {

        this.waitlistRepository = waitlistRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // Retrieve all waitlist entries for a selected date
    @Transactional(readOnly = true)
    public List<WaitlistEntry> getWaitlistsForDate(
            LocalDate appointmentDate) {

        if (appointmentDate == null) {
            throw new IllegalArgumentException(
                "Appointment date is required."
            );
        }

        List<WaitlistEntry> entries = waitlistRepository
            .findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(
                appointmentDate
            );

        // Track positions separately for each service and time
        Map<Long, Map<LocalTime, Integer>> positions =
            new HashMap<>();

        for (WaitlistEntry entry : entries) {

            entry.setQueuePosition(null);

            // Only WAITING entries count toward queue position
            if (!"WAITING".equals(entry.getStatus())) {
                continue;
            }

            Map<LocalTime, Integer> servicePositions =
                positions.computeIfAbsent(
                    entry.getServiceId(),
                    id -> new HashMap<>()
                );

            int position = servicePositions.merge(
                entry.getStartTime(),
                1,
                Integer::sum
            );

            entry.setQueuePosition(position);
        }

        return entries;
    }

    // Notify the first waiting customer after an appointment cancellation
    @Transactional
    public WaitlistEntry notifyNextCustomer(Long waitlistId) {

        if (waitlistId == null || waitlistId <= 0) {
            throw new IllegalArgumentException(
                "Waitlist entry not found."
            );
        }

        // Find the requested entry to identify its appointment slot
        WaitlistEntry requested = waitlistRepository
            .findById(waitlistId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Waitlist entry not found."
            ));

        // Lock the queue during this notification transaction
        List<WaitlistEntry> queue =
            waitlistRepository.findQueueForUpdate(
                requested.getServiceId(),
                requested.getAppointmentDate(),
                requested.getStartTime()
            );

        // Refresh locked entries so their statuses reflect
        // changes committed by other transactions.
        for (WaitlistEntry entry : queue) {
            entityManager.refresh(entry);
        }

        // Prevent multiple active notifications for one slot
        for (WaitlistEntry entry : queue) {

            if ("NOTIFIED".equals(entry.getStatus())) {
                throw new IllegalStateException(
                    "A customer has already been notified for this slot."
                );
            }
        }

        // Find the first customer who is still waiting
        WaitlistEntry next = null;

        for (WaitlistEntry entry : queue) {

            if ("WAITING".equals(entry.getStatus())) {
                next = entry;
                break;
            }
        }

        if (next == null) {
            throw new IllegalStateException(
                "No customers are waiting for this slot."
            );
        }

        // Staff cannot skip customers ahead in the queue
        if (!Objects.equals(
                next.getWaitlistId(),
                waitlistId)) {

            throw new IllegalStateException(
                "Only the first customer in the queue can be notified."
            );
        }

        LocalDate date = next.getAppointmentDate();
        LocalTime startTime = next.getStartTime();
        LocalTime endTime = next.getEndTime();
        Long serviceId = next.getServiceId();

        // Reject appointment times that have already passed
        LocalDateTime now = LocalDateTime.now(
            ZoneId.of("America/Vancouver")
        );

        if (!date.atTime(startTime).isAfter(now)) {
            throw new IllegalStateException(
                "Cannot notify customers for a past appointment time."
            );
        }

        // Verify that a matching appointment was cancelled
        boolean cancellationExists = appointmentRepository
            .findByAppointmentDateAndStatus(
                date,
                "CANCELLED"
            )
            .stream()
            .anyMatch(appointment ->
                Objects.equals(
                    appointment.getServiceId(),
                    serviceId
                )
                && Objects.equals(
                    appointment.getStartTime(),
                    startTime
                )
                && Objects.equals(
                    appointment.getEndTime(),
                    endTime
                )
            );

        if (!cancellationExists) {
            throw new IllegalStateException(
                "No cancelled appointment was found for this slot."
            );
        }

        // Ensure no other booked appointment overlaps this slot
        boolean alreadyBooked = appointmentRepository
            .existsByAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                date,
                "BOOKED",
                endTime,
                startTime
            );

        if (alreadyBooked) {
            throw new IllegalStateException(
                "This appointment slot has already been booked."
            );
        }

        // Notify the first eligible customer
        next.setStatus("NOTIFIED");

        return waitlistRepository.save(next);
    }
}