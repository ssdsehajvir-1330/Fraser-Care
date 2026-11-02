package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;

    public WaitlistService(WaitlistRepository waitlistRepository) {
        this.waitlistRepository = waitlistRepository;
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

        if (entry.getCustomerId() == null ||
            entry.getServiceId() == null ||
            entry.getAppointmentDate() == null ||
            entry.getStartTime() == null ||
            entry.getEndTime() == null) {

            throw new IllegalArgumentException(
                "Missing required waitlist information."
            );
        }

        if (entry.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                "Cannot join a waitlist for a past date."
            );
        }

        if (!entry.getEndTime().isAfter(entry.getStartTime())) {
            throw new IllegalArgumentException(
                "Invalid appointment time range."
            );
        }

        // Get existing entries for this appointment
        List<WaitlistEntry> existingEntries = getWaitlist(
            entry.getServiceId(),
            entry.getAppointmentDate(),
            entry.getStartTime()
        );

        // Stop same customer from joining twice
        for (WaitlistEntry existing : existingEntries) {
            if (existing.getCustomerId().equals(entry.getCustomerId())
                    && existing.getStatus().equals("WAITING")) {

                throw new IllegalStateException(
                    "Customer is already on this waitlist."
                );
            }
        }

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

        if (!status.equals("WAITING") &&
            !status.equals("NOTIFIED")) {

            throw new IllegalArgumentException(
                "Invalid waitlist status."
            );
        }

        entry.setStatus(status);

        return waitlistRepository.save(entry);
    }
}
