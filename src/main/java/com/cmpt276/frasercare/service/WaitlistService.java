package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import org.springframework.stereotype.Service;
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

        // Customers cannot join a waitlist for an available slot
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

    // Allow a customer to leave the waitlist
    public void leaveWaitlist(Long waitlistId) {

        WaitlistEntry entry = waitlistRepository.findById(waitlistId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Waitlist entry not found."
            ));

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

    // Retrieve all waitlists joined by a specific customer
    public List<WaitlistEntry> getCustomerWaitlists(Long customerId) {

        if (customerId == null) {
            throw new IllegalArgumentException(
                "Customer ID is required."
            );
        }

        return waitlistRepository.
            findByCustomerIdOrderByJoinedAtDescWaitlistIdDesc(customerId);
    }
}
