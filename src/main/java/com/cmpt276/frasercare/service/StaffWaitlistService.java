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
import java.util.LinkedHashMap;
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

    // Result of checking whether an entry can be notified
    public record NotificationEligibility(
        boolean ready,
        String reason
    ) {
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

    // Identify which customers are ready for staff approval
    @Transactional(readOnly = true)
    public Map<Long, NotificationEligibility>
            getNotificationEligibilityForDate(LocalDate date) {

        if (date == null) {
            throw new IllegalArgumentException(
                "Appointment date is required."
            );
        }

        List<WaitlistEntry> entries = waitlistRepository
            .findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(
                date
            );

        List<Appointment> cancelled = appointmentRepository
            .findByAppointmentDateAndStatus(date, "CANCELLED");

        List<Appointment> booked = appointmentRepository
            .findByAppointmentDateAndStatus(date, "BOOKED");

        return calculateEligibility(entries, cancelled, booked);
    }

    // Determine readiness using current appointment and waitlist data
    private Map<Long, NotificationEligibility> calculateEligibility(
            List<WaitlistEntry> entries,
            List<Appointment> cancelled,
            List<Appointment> booked) {

        Map<Long, NotificationEligibility> results =
            new LinkedHashMap<>();

        LocalDateTime now = LocalDateTime.now(
            ZoneId.of("America/Vancouver")
        );

        for (WaitlistEntry entry : entries) {

            NotificationEligibility eligibility =
                checkEligibility(
                    entry,
                    entries,
                    cancelled,
                    booked,
                    now
                );

            results.put(
                entry.getWaitlistId(),
                eligibility
            );
        }

        return results;
    }

    // Shared eligibility rules for recommendations and approvals
    private NotificationEligibility checkEligibility(
            WaitlistEntry entry,
            List<WaitlistEntry> allEntries,
            List<Appointment> cancelled,
            List<Appointment> booked,
            LocalDateTime now) {

        if ("NOTIFIED".equals(entry.getStatus())) {
            return blocked("Customer has already been notified.");
        }

        if (!"WAITING".equals(entry.getStatus())) {
            return blocked("Customer is not currently waiting.");
        }

        // Staff cannot skip earlier waiting customers
        for (WaitlistEntry other : allEntries) {

            if (Objects.equals(
                    other.getWaitlistId(),
                    entry.getWaitlistId())) {
                continue;
            }

            if (sameQueue(other, entry)
                    && "WAITING".equals(other.getStatus())
                    && isEarlier(other, entry)) {

                return blocked(
                    "Only the first customer in the queue can be notified."
                );
            }
        }

        // Reject past appointment times
        if (!entry.getAppointmentDate()
                .atTime(entry.getStartTime())
                .isAfter(now)) {

            return blocked(
                "Cannot notify customers for a past appointment time."
            );
        }

        // Require overlapping cancelled appointment
        boolean cancellationExists = cancelled.stream()
            .anyMatch(appointment ->
                overlaps(
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    entry.getStartTime(),
                    entry.getEndTime()
                )
            );

        if (!cancellationExists) {
            return blocked(
                "No cancelled appointment was found for this slot."
            );
        }

        // Recheck the customer own booked appointments
        boolean customerConflict = booked.stream()
            .anyMatch(appointment ->
                Objects.equals(
                    appointment.getCustomerId(),
                    entry.getCustomerId()
                )
                && overlaps(
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    entry.getStartTime(),
                    entry.getEndTime()
                )
            );

        if (customerConflict) {
            return blocked(
                "Customer already has a conflicting booked appointment."
            );
        }

        // No booked appointment may overlap the offered slot
        boolean alreadyBooked = booked.stream()
            .anyMatch(appointment ->
                overlaps(
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    entry.getStartTime(),
                    entry.getEndTime()
                )
            );

        if (alreadyBooked) {
            return blocked(
                "This appointment slot has already been booked."
            );
        }

        // Prevent two active notifications for overlapping slots even when services and queue start times different
        boolean overlappingNotification = allEntries.stream()
            .anyMatch(other ->
                "NOTIFIED".equals(other.getStatus())
                && overlaps(
                    other.getStartTime(),
                    other.getEndTime(),
                    entry.getStartTime(),
                    entry.getEndTime()
                )
            );

        if (overlappingNotification) {
            return blocked(
                "Another customer has already been notified for an overlapping slot."
            );
        }

        return new NotificationEligibility(
            true,
            "Ready to notify"
        );
    }

    // Determine whether two waitlist entries belong to same queue
    private boolean sameQueue(
            WaitlistEntry first,
            WaitlistEntry second) {

        return Objects.equals(
                first.getServiceId(),
                second.getServiceId()
            )
            && Objects.equals(
                first.getAppointmentDate(),
                second.getAppointmentDate()
            )
            && Objects.equals(
                first.getStartTime(),
                second.getStartTime()
            );
    }

    // Preserve FIFO ordering
    private boolean isEarlier(
            WaitlistEntry first,
            WaitlistEntry second) {

        LocalDateTime firstJoined = first.getJoinedAt();
        LocalDateTime secondJoined = second.getJoinedAt();

        if (firstJoined != null && secondJoined != null) {

            int comparison = firstJoined.compareTo(secondJoined);

            if (comparison != 0) {
                return comparison < 0;
            }
        }

        // Use the entry ID to break ties
        return first.getWaitlistId() != null
            && second.getWaitlistId() != null
            && first.getWaitlistId() < second.getWaitlistId();
    }

    // Time intervals overlap only when they share actual time
    private boolean overlaps(
            LocalTime firstStart,
            LocalTime firstEnd,
            LocalTime secondStart,
            LocalTime secondEnd) {

        return firstStart.isBefore(secondEnd)
            && firstEnd.isAfter(secondStart);
    }

    private NotificationEligibility blocked(String reason) {
        return new NotificationEligibility(false, reason);
    }

    // Staff approve a notification after all rules are rechecked
    @Transactional
    public WaitlistEntry notifyNextCustomer(Long waitlistId) {

        if (waitlistId == null || waitlistId <= 0) {
            throw new IllegalArgumentException(
                "Waitlist entry not found."
            );
        }

        WaitlistEntry requested = waitlistRepository
            .findById(waitlistId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Waitlist entry not found."
            ));

        LocalDate date = requested.getAppointmentDate();

        // Lock all existing entries for date, coordinates approvals across overlapping queues
        List<WaitlistEntry> entries = waitlistRepository
            .findAllForDateForUpdate(date);

        // Refresh entries after acquiring locks
        for (WaitlistEntry entry : entries) {
            entityManager.refresh(entry);
        }

        WaitlistEntry target = entries.stream()
            .filter(entry ->
                Objects.equals(
                    entry.getWaitlistId(),
                    waitlistId
                )
            )
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Waitlist entry not found."
            ));

        List<Appointment> cancelled = appointmentRepository
            .findByAppointmentDateAndStatus(date, "CANCELLED");

        List<Appointment> booked = appointmentRepository
            .findByAppointmentDateAndStatus(date, "BOOKED");

        // Use exactly the same checks as the dashboard
        Map<Long, NotificationEligibility> eligibility =
            calculateEligibility(
                entries,
                cancelled,
                booked
            );

        NotificationEligibility decision =
            eligibility.get(waitlistId);

        if (decision == null || !decision.ready()) {
            throw new IllegalStateException(
                decision == null
                    ? "Customer is not eligible for notification."
                    : decision.reason()
            );
        }

        // Final staff approved status change
        target.setStatus("NOTIFIED");

        return waitlistRepository.save(target);
    }
}