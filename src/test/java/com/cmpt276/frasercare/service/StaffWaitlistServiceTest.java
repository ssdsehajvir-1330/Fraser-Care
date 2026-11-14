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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
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

        // Mock the JPA EntityManager used for refreshing locked rows
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

        when(waitlists.findQueueForUpdate(
            3L,
            date,
            LocalTime.of(12, 30)
        )).thenReturn(List.of(first, second));

        // Create a matching cancelled appointment
        Appointment cancelled = new Appointment();

        cancelled.setServiceId(3L);
        cancelled.setAppointmentDate(date);
        cancelled.setStartTime(LocalTime.of(12, 30));
        cancelled.setEndTime(LocalTime.of(12, 45));
        cancelled.setStatus("CANCELLED");

        when(appointments.findByAppointmentDateAndStatus(
            date,
            "CANCELLED"
        )).thenReturn(List.of(cancelled));

        when(appointments
            .existsByAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                date,
                "BOOKED",
                LocalTime.of(12, 45),
                LocalTime.of(12, 30)
            )
        ).thenReturn(false);

        when(waitlists.save(first)).thenReturn(first);
    }

    private WaitlistEntry createEntry(
            Long id,
            String status) {

        WaitlistEntry entry = new WaitlistEntry();

        entry.setWaitlistId(id);
        entry.setCustomerId(id + 10);
        entry.setServiceId(3L);
        entry.setAppointmentDate(date);
        entry.setStartTime(LocalTime.of(12, 30));
        entry.setEndTime(LocalTime.of(12, 45));
        entry.setStatus(status);

        return entry;
    }

    @Test
    void notifiesFirstWaitingCustomer() {

        WaitlistEntry result =
            service.notifyNextCustomer(1L);

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

        when(appointments
            .existsByAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                date,
                "BOOKED",
                LocalTime.of(12, 45),
                LocalTime.of(12, 30)
            )
        ).thenReturn(true);

        assertThrows(
            IllegalStateException.class,
            () -> service.notifyNextCustomer(1L)
        );

        verify(waitlists, never()).save(any());
    }

    @Test
    void cannotNotifyWithoutCancellation() {

        when(appointments.findByAppointmentDateAndStatus(
            date,
            "CANCELLED"
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
    }

    @Test
    void calculatesWaitingPositions() {

        when(waitlists
            .findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(
                date
            )
        ).thenReturn(List.of(first, second));

        List<WaitlistEntry> entries =
            service.getWaitlistsForDate(date);

        assertEquals(1, entries.get(0).getQueuePosition());
        assertEquals(2, entries.get(1).getQueuePosition());
    }

    @Test
    void notifiedCustomerHasNoQueuePosition() {

        first.setStatus("NOTIFIED");

        when(waitlists
            .findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(
                date
            )
        ).thenReturn(List.of(first, second));

        List<WaitlistEntry> entries =
            service.getWaitlistsForDate(date);

        assertNull(entries.get(0).getQueuePosition());
        assertEquals(1, entries.get(1).getQueuePosition());
    }
}
