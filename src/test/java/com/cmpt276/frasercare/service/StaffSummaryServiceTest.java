package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.dto.StaffSummaryResponse;
import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.AppointmentRepository;
import com.cmpt276.frasercare.repository.PickupRequestRepository;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaffSummaryServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 25);

    private AppointmentRepository appointmentRepository;
    private PickupRequestRepository pickupRepository;
    private WaitlistRepository waitlistRepository;
    private StaffSummaryService summaryService;

    @BeforeEach
    void setUp() {
        appointmentRepository = Mockito.mock(AppointmentRepository.class);
        pickupRepository = Mockito.mock(PickupRequestRepository.class);
        waitlistRepository = Mockito.mock(WaitlistRepository.class);
        summaryService = new StaffSummaryService(
                appointmentRepository, pickupRepository, waitlistRepository);
    }

    // Helper to make a waitlist entry with a given status
    private WaitlistEntry entryWithStatus(String status) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setStatus(status);
        return entry;
    }

    // Counts booked appointments, open pickups and waiting customers
    @Test
    void getSummary_countsEverything() {
        Mockito.when(appointmentRepository.findByAppointmentDateAndStatus(DAY, "BOOKED"))
                .thenReturn(List.of(new Appointment(), new Appointment()));

        Mockito.when(pickupRepository.countByStatus("RECEIVED")).thenReturn(3L);
        Mockito.when(pickupRepository.countByStatus("READY")).thenReturn(1L);

        Mockito.when(waitlistRepository
                        .findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(DAY))
                .thenReturn(List.of(
                        entryWithStatus("WAITING"),
                        entryWithStatus("WAITING"),
                        entryWithStatus("NOTIFIED")));

        StaffSummaryResponse summary = summaryService.getSummary(DAY);

        assertEquals(DAY, summary.date());
        assertEquals(2, summary.appointmentsToday());
        assertEquals(3, summary.pickupsReceived());
        assertEquals(1, summary.pickupsReady());
        assertEquals(2, summary.waitlistToday());
    }

    // A quiet day shows all zeros
    @Test
    void getSummary_emptyDay_allZeros() {
        StaffSummaryResponse summary = summaryService.getSummary(DAY);

        assertEquals(0, summary.appointmentsToday());
        assertEquals(0, summary.pickupsReceived());
        assertEquals(0, summary.pickupsReady());
        assertEquals(0, summary.waitlistToday());
    }

    // Missing date is rejected
    @Test
    void getSummary_nullDate_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> summaryService.getSummary(null));
    }
}