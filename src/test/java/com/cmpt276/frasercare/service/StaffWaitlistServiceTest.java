package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.AppointmentRepository;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffWaitlistServiceTest {

    private WaitlistRepository waitlists;
    private AppointmentRepository appointments;
    private StaffWaitlistService service;

    private LocalDate date;
    private WaitlistEntry first;
    private WaitlistEntry second;

    @BeforeEach
    void setUp() {
        waitlists = mock(WaitlistRepository.class);
        appointments = mock(AppointmentRepository.class);

        service = new StaffWaitlistService(
            waitlists,
            appointments
        );

        EntityManager entityManager = mock(EntityManager.class);

        ReflectionTestUtils.setField(
            service,
            "entityManager",
            entityManager
        );

        date = LocalDate.now(
            ZoneId.of("America/Vancouver")
        ).plusDays(2);

        first = createEntry(1L, "WAITING");
        second = createEntry(2L, "WAITING");

        when(waitlists.findById(1L))
            .thenReturn(Optional.of(first));

        when(waitlists.findById(2L))
            .thenReturn(Optional.of(second));

        when(waitlists.findAllForDateForUpdate(date))
            .thenReturn(List.of(first, second));

        when(waitlists
            .findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(
                date
            )
        ).thenReturn(List.of(first, second));

        when(appointments.findByAppointmentDateAndStatus(
            date, "CANCELLED"
        )).thenReturn(List.of(
            createAppointment(
                3L, 99L,
                LocalTime.of(12, 30),
                LocalTime.of(12, 45),
                "CANCELLED"
            )
        ));

        when(appointments.findByAppointmentDateAndStatus(
            date, "BOOKED"
        )).thenReturn(List.of());

        when(waitlists.save(first)).thenReturn(first);
    }

    // Create a waitlist entry for testing
    private WaitlistEntry createEntry(Long id, String status) {
        WaitlistEntry entry = new WaitlistEntry();

        entry.setWaitlistId(id);
        entry.setCustomerId(id + 10);
        entry.setServiceId(3L);
        entry.setAppointmentDate(date);
        entry.setStartTime(LocalTime.of(12, 30));
        entry.setEndTime(LocalTime.of(12, 45));

        entry.setJoinedAt(
            LocalDateTime.of(date, LocalTime.of(9, 0))
                .plusMinutes(id)
        );

        entry.setStatus(status);

        return entry;
    }

    // Create an appointment for testing
    private Appointment createAppointment(
            Long serviceId,
            Long customerId,
            LocalTime startTime,
            LocalTime endTime,
            String status) {

        Appointment appointment = new Appointment();

        appointment.setServiceId(serviceId);
        appointment.setCustomerId(customerId);
        appointment.setAppointmentDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(status);

        return appointment;
    }

    @Test
    void notifiesFirstWaitingCustomer() {
        WaitlistEntry result = service.notifyNextCustomer(1L);

        assertEquals("NOTIFIED", result.getStatus());
        verify(waitlists).save(first);
    }

    @Test
    void cannotSkipFirstCustomer() {
        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(2L)
        );

        verify(waitlists, never()).save(any());
    }

    @Test
    void cannotNotifyWhileAnotherCustomerIsNotified() {
        first.setStatus("NOTIFIED");

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(2L)
        );

        verify(waitlists, never()).save(any());
    }

    @Test
    void cannotNotifyIfSlotWasRebooked() {
        Appointment booked = createAppointment(
            3L, 99L,
            LocalTime.of(12, 30),
            LocalTime.of(12, 45),
            "BOOKED"
        );

        when(appointments.findByAppointmentDateAndStatus(
            date, "BOOKED"
        )).thenReturn(List.of(booked));

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(1L)
        );

        verify(waitlists, never()).save(any());
    }

    @Test
    void cannotNotifyWithoutCancellation() {
        when(appointments.findByAppointmentDateAndStatus(
            date, "CANCELLED"
        )).thenReturn(List.of());

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(1L)
        );

        verify(waitlists, never()).save(any());
    }

    @Test
    void cannotNotifySameCustomerTwice() {
        service.notifyNextCustomer(1L);

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(1L)
        );

        verify(waitlists, times(1)).save(first);
    }

    @Test
    void calculatesWaitingPositions() {
        List<WaitlistEntry> entries =
            service.getWaitlistsForDate(date);

        assertEquals(1, entries.get(0).getQueuePosition());
        assertEquals(2, entries.get(1).getQueuePosition());
    }

    @Test
    void notifiedCustomerHasNoQueuePosition() {
        first.setStatus("NOTIFIED");

        List<WaitlistEntry> entries =
            service.getWaitlistsForDate(date);

        assertNull(entries.get(0).getQueuePosition());
        assertEquals(1, entries.get(1).getQueuePosition());
    }

    @Test
    void notifiesWhenDifferentServiceCancellationOverlaps() {
        Appointment cancelled = createAppointment(
            4L, 99L,
            LocalTime.of(12, 15),
            LocalTime.of(13, 0),
            "CANCELLED"
        );

        when(appointments.findByAppointmentDateAndStatus(
            date, "CANCELLED"
        )).thenReturn(List.of(cancelled));

        Map<Long, StaffWaitlistService.NotificationEligibility> results =
            service.getNotificationEligibilityForDate(date);

        assertTrue(results.get(1L).ready());

        WaitlistEntry result = service.notifyNextCustomer(1L);

        assertEquals("NOTIFIED", result.getStatus());
        verify(waitlists).save(first);
    }

    @Test
    void notifiesWhenCancellationPartiallyOverlaps() {
        Appointment cancelled = createAppointment(
            4L, 99L,
            LocalTime.of(12, 40),
            LocalTime.of(13, 0),
            "CANCELLED"
        );

        when(appointments.findByAppointmentDateAndStatus(
            date, "CANCELLED"
        )).thenReturn(List.of(cancelled));

        WaitlistEntry result = service.notifyNextCustomer(1L);

        assertEquals("NOTIFIED", result.getStatus());
    }

    @Test
    void cannotNotifyWhenCancellationEndsAtSlotStart() {
        Appointment cancelled = createAppointment(
            4L, 99L,
            LocalTime.of(12, 0),
            LocalTime.of(12, 30),
            "CANCELLED"
        );

        when(appointments.findByAppointmentDateAndStatus(
            date, "CANCELLED"
        )).thenReturn(List.of(cancelled));

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(1L)
        );

        verify(waitlists, never()).save(any());
    }

    @Test
    void cannotNotifyWhenCancellationStartsAtSlotEnd() {
        Appointment cancelled = createAppointment(
            4L, 99L,
            LocalTime.of(12, 45),
            LocalTime.of(13, 0),
            "CANCELLED"
        );

        when(appointments.findByAppointmentDateAndStatus(
            date, "CANCELLED"
        )).thenReturn(List.of(cancelled));

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(1L)
        );

        verify(waitlists, never()).save(any());
    }

    @Test
    void eligibilityIdentifiesFirstCustomerAsReady() {
        Map<Long, StaffWaitlistService.NotificationEligibility> results =
            service.getNotificationEligibilityForDate(date);

        assertTrue(results.get(1L).ready());
        assertFalse(results.get(2L).ready());

        assertEquals(
            "Ready to notify",
            results.get(1L).reason()
        );

        assertTrue(
            results.get(2L).reason().contains("first customer")
        );
    }

    @Test
    void cannotNotifyCustomerWithConflictingBooking() {
        Appointment booked = createAppointment(
            4L,
            first.getCustomerId(),
            LocalTime.of(12, 25),
            LocalTime.of(12, 40),
            "BOOKED"
        );

        when(appointments.findByAppointmentDateAndStatus(
            date, "BOOKED"
        )).thenReturn(List.of(booked));

        Map<Long, StaffWaitlistService.NotificationEligibility> results =
            service.getNotificationEligibilityForDate(date);

        assertFalse(results.get(1L).ready());
        assertEquals(
            "Customer already has a conflicting booked appointment.",
            results.get(1L).reason()
        );

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(1L)
        );

        verify(waitlists, never()).save(any());
    }

    @Test
    void cannotNotifyWhenOtherServiceHasOverlappingNotification() {
        WaitlistEntry otherNotified = createEntry(9L, "NOTIFIED");

        otherNotified.setServiceId(4L);
        otherNotified.setStartTime(LocalTime.of(12, 35));
        otherNotified.setEndTime(LocalTime.of(12, 50));

        List<WaitlistEntry> entries =
            List.of(first, second, otherNotified);

        when(waitlists.findAllForDateForUpdate(date))
            .thenReturn(entries);

        when(waitlists
            .findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(
                date
            )
        ).thenReturn(entries);

        Map<Long, StaffWaitlistService.NotificationEligibility> results =
            service.getNotificationEligibilityForDate(date);

        assertFalse(results.get(1L).ready());

        assertEquals(
            "Another customer has already been notified for an overlapping slot.",
            results.get(1L).reason()
        );

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(1L)
        );

        verify(waitlists, never()).save(any());
    }

    // Test 16: Skipping 1st customer records reason and advances queue
    @Test
    void skipFirstCustomer_movesNextCustomerForward() {
        WaitlistEntry result = service.skipWaitlistEntry(
            1L,
            "Customer declined"
        );

        assertEquals("SKIPPED", result.getStatus());
        assertEquals("Customer declined", result.getSkipReason());

        verify(waitlists).save(first);

        List<WaitlistEntry> entries =
            service.getWaitlistsForDate(date);

        assertNull(entries.get(0).getQueuePosition());
        assertEquals(1, entries.get(1).getQueuePosition());

        Map<Long, StaffWaitlistService.NotificationEligibility> eligibility =
            service.getNotificationEligibilityForDate(date);

        assertTrue(eligibility.get(2L).ready());
    }

    // Test 17: Cannot skip customer out of order or without valid reason
    @Test
    void rejectsInvalidSkipRequests() {
        assertThrows(
            IllegalStateException.class,
            () -> service.skipWaitlistEntry(
                2L,
                "Customer declined"
            )
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.skipWaitlistEntry(
                1L,
                "Other reason"
            )
        );

        assertEquals("WAITING", first.getStatus());
        assertEquals("WAITING", second.getStatus());

        verify(waitlists, never()).save(any());
    }

    // Test 18: Skipping notified customer allows queue to continue
    @Test
    void skipNotifiedCustomer_allowsNextNotification() {
        first.setStatus("NOTIFIED");

        Map<Long, StaffWaitlistService.NotificationEligibility> before =
            service.getNotificationEligibilityForDate(date);

        assertFalse(before.get(2L).ready());

        WaitlistEntry skipped = service.skipWaitlistEntry(
            1L,
            "Unable to contact"
        );

        assertEquals("SKIPPED", skipped.getStatus());
        assertEquals("Unable to contact", skipped.getSkipReason());

        Map<Long, StaffWaitlistService.NotificationEligibility> after =
            service.getNotificationEligibilityForDate(date);

        assertTrue(after.get(2L).ready());

        verify(waitlists).save(first);
    }
}