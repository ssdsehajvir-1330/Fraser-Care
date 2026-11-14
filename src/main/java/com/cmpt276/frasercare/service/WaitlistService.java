package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final AppointmentBookingService appointmentBookingService;

    public WaitlistService(
            WaitlistRepository waitlistRepository,
            AppointmentBookingService appointmentBookingService) {

        this.waitlistRepository = waitlistRepository;
        this.appointmentBookingService = appointmentBookingService;
    }

    // Retrieve customers waiting for a particular appointment time
    public List<WaitlistEntry> getWaitlist(
            Long serviceId,
            LocalDate appointmentDate,
            LocalTime startTime) {

        return waitlistRepository
            .findByServiceIdAndAppointmentDateAndStartTimeOrderByJoinedAtAscWaitlistIdAsc(
                serviceId, appointmentDate, startTime
            );
    }

    // Retrieve a customer's waitlists and calculate queue positions
    public List<WaitlistEntry> getCustomerWaitlists(Long customerId) {

        if (customerId == null) {
            throw new IllegalArgumentException(
                "Customer ID is required."
            );
        }

        List<WaitlistEntry> entries = waitlistRepository
            .findByCustomerIdOrderByJoinedAtDescWaitlistIdDesc(
                customerId
            );

        // Calculate the position for each waitlist entry
        for (WaitlistEntry entry : entries) {

            // Only WAITING entries have a queue position
            entry.setQueuePosition(null);

            if (!"WAITING".equals(entry.getStatus())) {
                continue;
            }

            // Retrieve everyone waiting for the same appointment
            List<WaitlistEntry> queue = getWaitlist(
                entry.getServiceId(),
                entry.getAppointmentDate(),
                entry.getStartTime()
            );

            int position = 0;

            // The queue is already sorted oldest to newest
            for (WaitlistEntry queuedEntry : queue) {

                // Skip customers who are no longer waiting
                if (!"WAITING".equals(queuedEntry.getStatus())) {
                    continue;
                }

                position++;

                // Find this customer's position
                if (queuedEntry.getWaitlistId().equals(
                        entry.getWaitlistId())) {

                    entry.setQueuePosition(position);
                    break;
                }
            }
        }

        return entries;
    }

    // Add customer to the waitlist
    public WaitlistEntry joinWaitlist(WaitlistEntry entry) {

        // Validate required information
        if (entry == null ||
            entry.getCustomerId() == null ||
            entry.getServiceId() == null ||
            entry.getAppointmentDate() == null ||
            entry.getStartTime() == null ||
            entry.getEndTime() == null) {

            throw new IllegalArgumentException(
                "Missing required waitlist information."
            );
        }

        // Reject past dates using Vancouver time
        if (entry.getAppointmentDate().isBefore(
                LocalDate.now(ZoneId.of("America/Vancouver")))) {

            throw new IllegalArgumentException(
                "Cannot join a waitlist for a past date."
            );
        }

        // Validate appointment time range
        if (!entry.getEndTime().isAfter(entry.getStartTime())) {
            throw new IllegalArgumentException(
                "Invalid appointment time range."
            );
        }

        // Check that the requested slot is valid and already booked
        boolean eligible =
            appointmentBookingService.isWaitlistEligibleSlot(
                entry.getServiceId(),
                entry.getAppointmentDate(),
                entry.getStartTime(),
                entry.getEndTime()
            );

        if (!eligible) {
            throw new IllegalStateException(
                "This appointment slot is available. Please book it instead."
            );
        }

        // Get existing entries for this appointment
        List<WaitlistEntry> existingEntries = getWaitlist(
            entry.getServiceId(),
            entry.getAppointmentDate(),
            entry.getStartTime()
        );

        // Stop the same customer from joining twice
        for (WaitlistEntry existing : existingEntries) {
            if (existing.getCustomerId().equals(entry.getCustomerId())
                    && "WAITING".equals(existing.getStatus())) {

                throw new IllegalStateException(
                    "Customer is already on this waitlist."
                );
            }
        }

        // Create a new waiting entry
        entry.setWaitlistId(null);
        entry.setJoinedAt(null);
        entry.setStatus("WAITING");

        return waitlistRepository.save(entry);
    }

    // A customer can remove only their own active waitlist entry
    @Transactional
    public void leaveWaitlist(Long waitlistId, Long customerId) {

        if (waitlistId == null || customerId == null) {
            throw new IllegalArgumentException("Waitlist entry not found.");
        }

        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Waitlist entry not found."
            ));

        if (!customerId.equals(entry.getCustomerId())) {
            // Same response for unknown IDs and other users' entries
            throw new IllegalArgumentException("Waitlist entry not found.");
        }

        // Historical/completed entries should not be deleted
        if (!"WAITING".equals(entry.getStatus()) &&
            !"NOTIFIED".equals(entry.getStatus())) {
            throw new IllegalStateException(
                "Only waiting or notified entries can be removed."
            );
        }

        waitlistRepository.delete(entry);
    }

    // Allow staff to update customer waitlist status
    public WaitlistEntry updateStatus(Long waitlistId, String status) {

        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Waitlist entry not found."
            ));

        if (!"WAITING".equals(status) &&
            !"NOTIFIED".equals(status)) {

            throw new IllegalArgumentException(
                "Invalid waitlist status."
            );
        }

        entry.setStatus(status);

        return waitlistRepository.save(entry);
    }
}
