package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
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
        entry.setAppointmentDate(
            LocalDate.now(ZoneId.of("America/Vancouver")).minusDays(1)
        );

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

    // Test 6: Reject duplicate waitlist registration
    @Test
    void joinWaitlist_duplicateRegistration_throwsException() {
        WaitlistEntry entry = createValidEntry();

        // Existing WAITING entry for the same customer and slot
        WaitlistEntry existingEntry = createValidEntry();
        existingEntry.setStatus("WAITING");

        Mockito.when(appointmentBookingService.isWaitlistEligibleSlot(
            entry.getServiceId(),
            entry.getAppointmentDate(),
            entry.getStartTime(),
            entry.getEndTime()
        )).thenReturn(true);

        Mockito.when(
            waitlistRepository
                .findByServiceIdAndAppointmentDateAndStartTimeOrderByJoinedAtAscWaitlistIdAsc(
                    entry.getServiceId(),
                    entry.getAppointmentDate(),
                    entry.getStartTime()
                )
        ).thenReturn(List.of(existingEntry));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> waitlistService.joinWaitlist(entry)
        );

        assertEquals(
            "Customer is already on this waitlist.",
            exception.getMessage()
        );

        // Verify that duplicate registration does not save another entry
        Mockito.verify(waitlistRepository, Mockito.never())
            .save(Mockito.any(WaitlistEntry.class));
    }

    // Test 7: Correctly return number of waiting customers
    @Test
    void getWaitingCount_threeWaitingCustomers_returnsThree() {
        Long serviceId = 1L;
        LocalDate date = LocalDate.of(2026, 12, 1);
        LocalTime startTime = LocalTime.of(14, 0);

        Mockito.when(
            waitlistRepository
                .countByServiceIdAndAppointmentDateAndStartTimeAndStatus(
                    serviceId, date, startTime, "WAITING"
                )
        ).thenReturn(3L);

        long count = waitlistService.getWaitingCount(
            serviceId, date, startTime
        );

        assertEquals(3L, count);

        Mockito.verify(waitlistRepository)
            .countByServiceIdAndAppointmentDateAndStartTimeAndStatus(
                serviceId, date, startTime, "WAITING"
            );
    }

    // Test 8: Return zero when nobody is waiting
    @Test
    void getWaitingCount_emptyWaitlist_returnsZero() {
        Long serviceId = 1L;
        LocalDate date = LocalDate.of(2026, 12, 1);
        LocalTime startTime = LocalTime.of(14, 0);

        Mockito.when(
            waitlistRepository
                .countByServiceIdAndAppointmentDateAndStartTimeAndStatus(
                    serviceId, date, startTime, "WAITING"
                )
        ).thenReturn(0L);

        long count = waitlistService.getWaitingCount(
            serviceId, date, startTime
        );

        assertEquals(0L, count);
    }

    // Test 9: Reject incomplete appointment information
    @Test
    void getWaitingCount_missingInformation_throwsException() {
        LocalDate date = LocalDate.of(2026, 12, 1);
        LocalTime startTime = LocalTime.of(14, 0);

        assertThrows(IllegalArgumentException.class,
            () -> waitlistService.getWaitingCount(
                null, date, startTime
            ));

        assertThrows(IllegalArgumentException.class,
            () -> waitlistService.getWaitingCount(
                1L, null, startTime
            ));

        assertThrows(IllegalArgumentException.class,
            () -> waitlistService.getWaitingCount(
                1L, date, null
            ));

        Mockito.verifyNoInteractions(waitlistRepository);
    }

    // Helper to create valid test data
    private WaitlistEntry createValidEntry() {
        WaitlistEntry entry = new WaitlistEntry();

        entry.setCustomerId(1L);
        entry.setServiceId(1L);
        entry.setAppointmentDate(
            LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(1)
        );
        entry.setStartTime(LocalTime.of(14, 0));
        entry.setEndTime(LocalTime.of(14, 15));

        return entry;
    }
}