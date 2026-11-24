package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.dto.StaffSummaryResponse;
import com.cmpt276.frasercare.repository.AppointmentRepository;
import com.cmpt276.frasercare.repository.PickupRequestRepository;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class StaffSummaryService {

    private static final ZoneId TIME_ZONE = ZoneId.of("America/Vancouver");

    private final AppointmentRepository appointmentRepository;
    private final PickupRequestRepository pickupRepository;
    private final WaitlistRepository waitlistRepository;

    public StaffSummaryService(
            AppointmentRepository appointmentRepository,
            PickupRequestRepository pickupRepository,
            WaitlistRepository waitlistRepository) {
        this.appointmentRepository = appointmentRepository;
        this.pickupRepository = pickupRepository;
        this.waitlistRepository = waitlistRepository;
    }

    // Today's numbers for the staff dashboard
    public StaffSummaryResponse getTodaySummary() {
        return getSummary(LocalDate.now(TIME_ZONE));
    }

    // Numbers for a given day
    public StaffSummaryResponse getSummary(LocalDate date) {

        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        long appointments = appointmentRepository
                .findByAppointmentDateAndStatus(date, "BOOKED")
                .size();

        long waiting = waitlistRepository
                .findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(date)
                .stream()
                .filter(entry -> "WAITING".equals(entry.getStatus()))
                .count();

        return new StaffSummaryResponse(
                date,
                appointments,
                pickupRepository.countByStatus("RECEIVED"),
                pickupRepository.countByStatus("READY"),
                waiting);
    }
}