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

    @BeforeEach
    void setUp() {
        waitlistRepository = Mockito.mock(WaitlistRepository.class);
        waitlistService = new WaitlistService(waitlistRepository);
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
