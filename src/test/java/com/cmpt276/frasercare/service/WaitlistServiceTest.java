package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class WaitlistServiceTest {

    private WaitlistRepository waitlistRepository;
    private WaitlistService waitlistService;
    private AppointmentBookingService appointmentBookingService;

    @BeforeEach
    void setUp() {
        waitlistRepository = Mockito.mock(WaitlistRepository.class);

        appointmentBookingService =
            Mockito.mock(AppointmentBookingService.class);

        waitlistService = new WaitlistService(
            waitlistRepository,
            appointmentBookingService
        );
    }

    // Test 1: Reject missing required information
    @Test
    void joinWaitlist_missingCustomerId_throwsException() {
        WaitlistEntry entry = createValidEntry();
        entry.setCustomerId(null);

        assertThrows(IllegalArgumentException.class,
            () -> waitlistService.joinWaitlist(entry));
    }

    // Test 2: Reject appointment dates in the past
    @Test
    void joinWaitlist_pastDate_throwsException() {
        WaitlistEntry entry = createValidEntry();
        entry.setAppointmentDate(LocalDate.now().minusDays(1));

        assertThrows(IllegalArgumentException.class,
            () -> waitlistService.joinWaitlist(entry));
    }

    // Test 3: Reject an end time before the start time
    @Test
    void joinWaitlist_invalidTimeRange_throwsException() {
        WaitlistEntry entry = createValidEntry();
        entry.setEndTime(LocalTime.of(13, 0));

        assertThrows(IllegalArgumentException.class,
            () -> waitlistService.joinWaitlist(entry));
    }

    // Test 4: Reject an available appointment slot
    @Test
    void joinWaitlist_availableSlot_throwsException() {
        WaitlistEntry entry = createValidEntry();

        // False means the slot is not occupied by a booking
        Mockito.when(appointmentBookingService.isWaitlistEligibleSlot(
            entry.getServiceId(),
            entry.getAppointmentDate(),
            entry.getStartTime(),
            entry.getEndTime()
        )).thenReturn(false);

        assertThrows(IllegalStateException.class,
            () -> waitlistService.joinWaitlist(entry));
    }

    // Test 5: Allow joining the waitlist for a booked slot
    @Test
    void joinWaitlist_fullyBookedSlot_savesEntry() {
        WaitlistEntry entry = createValidEntry();

        // True means the slot is valid and occupied
        Mockito.when(appointmentBookingService.isWaitlistEligibleSlot(
            entry.getServiceId(),
            entry.getAppointmentDate(),
            entry.getStartTime(),
            entry.getEndTime()
        )).thenReturn(true);

        Mockito.when(waitlistRepository.save(entry))
            .thenReturn(entry);

        WaitlistEntry result = waitlistService.joinWaitlist(entry);

        assertNotNull(result);
        assertEquals("WAITING", result.getStatus());

        Mockito.verify(waitlistRepository).save(entry);
    }

    // Helper to create valid test data
    private WaitlistEntry createValidEntry() {
        WaitlistEntry entry = new WaitlistEntry();

        entry.setCustomerId(1L);
        entry.setServiceId(1L);
        entry.setAppointmentDate(LocalDate.now().plusDays(1));
        entry.setStartTime(LocalTime.of(14, 0));
        entry.setEndTime(LocalTime.of(14, 15));

        return entry;
    }
}
