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

    public record NotificationEligibility(
        boolean ready,
        String reason
    ) {
    }

    // Retrieve waitlists and calculate FIFO queue positions
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

    // Automatically identify entries eligible for staff approval
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

            results.put(
                entry.getWaitlistId(),
                checkEligibility(
                    entry,
                    entries,
                    cancelled,
                    booked,
                    now
                )
            );
        }

        return results;
    }

    // Shared rules used for recommendations and staff approval
    private NotificationEligibility checkEligibility(
            WaitlistEntry entry,
            List<WaitlistEntry> allEntries,
            List<Appointment> cancelled,
            List<Appointment> booked,
            LocalDateTime now) {

        if ("NOTIFIED".equals(entry.getStatus())) {
            return blocked(
                "Customer has already been notified."
            );
        }

        if (!"WAITING".equals(entry.getStatus())) {
            return blocked(
                "Customer is not currently waiting."
            );
        }

        // Preserve FIFO order in each queue
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

        // Cancellation must overlap requested slot
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

        // Check for customer's own conflicting booking
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

        // Make sure slot still available
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

        // Prevent conflicting notifications across services
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

    // Compare waitlist join times with id being the tiebreaker for entries created at same time
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

        return first.getWaitlistId() != null
            && second.getWaitlistId() != null
            && first.getWaitlistId() < second.getWaitlistId();
    }

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

    // Staff approve notification after rechecking eligibility
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

        List<WaitlistEntry> entries =
            waitlistRepository.findAllForDateForUpdate(date);

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

        target.setStatus("NOTIFIED");

        return waitlistRepository.save(target);
    }

    // Staff can skip first waiting or notified customer when they decline or cannot be contacted
    @Transactional
    public WaitlistEntry skipWaitlistEntry(
            Long waitlistId,
            String reason) {

        if (waitlistId == null || waitlistId <= 0) {
            throw new IllegalArgumentException(
                "Waitlist entry not found."
            );
        }

        // Require a recognized reason for the staff override
        if (!"Customer declined".equals(reason)
                && !"Unable to contact".equals(reason)) {

            throw new IllegalArgumentException(
                "A valid skip reason is required."
            );
        }

        WaitlistEntry requested = waitlistRepository
            .findById(waitlistId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Waitlist entry not found."
            ));

        // Use same date-wide lock as notification approval
        List<WaitlistEntry> entries =
            waitlistRepository.findAllForDateForUpdate(
                requested.getAppointmentDate()
            );

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

        String status = target.getStatus();

        // Only active entries may be skipped
        if (!"WAITING".equals(status)
                && !"NOTIFIED".equals(status)) {

            throw new IllegalStateException(
                "Only waiting or notified customers can be skipped."
            );
        }

        // A waiting customer must be first in queue
        // An already notified customer can be skipped so the next becomes eligible
        if ("WAITING".equals(status)) {

            boolean customerAhead = entries.stream()
                .anyMatch(other ->
                    !Objects.equals(
                        other.getWaitlistId(),
                        target.getWaitlistId()
                    )
                    && sameQueue(other, target)
                    && "WAITING".equals(other.getStatus())
                    && isEarlier(other, target)
                );

            if (customerAhead) {
                throw new IllegalStateException(
                    "Only the first waiting customer can be skipped."
                );
            }

            boolean alreadyNotified = entries.stream()
                .anyMatch(other ->
                    sameQueue(other, target)
                    && "NOTIFIED".equals(other.getStatus())
                );

            if (alreadyNotified) {
                throw new IllegalStateException(
                    "A customer has already been notified for this slot."
                );
            }
        }

        // Record override and remove entry from queue
        target.setStatus("SKIPPED");
        target.setSkipReason(reason);

        return waitlistRepository.save(target);
    }
}