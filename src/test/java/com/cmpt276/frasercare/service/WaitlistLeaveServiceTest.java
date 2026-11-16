package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.WaitlistEntry;
import com.cmpt276.frasercare.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WaitlistLeaveServiceTest {

    private WaitlistRepository repository;
    private WaitlistService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(WaitlistRepository.class);
        AppointmentBookingService appointments =
            Mockito.mock(AppointmentBookingService.class);
        service = new WaitlistService(repository, appointments);
    }

    private WaitlistEntry entry(Long ownerId, String status) {
        WaitlistEntry result = new WaitlistEntry();
        result.setWaitlistId(7L);
        result.setCustomerId(ownerId);
        result.setStatus(status);
        return result;
    }

    @Test
    void ownerCanLeaveWaitingEntry() {
        WaitlistEntry entry = entry(3L, "WAITING");
        when(repository.findById(7L)).thenReturn(Optional.of(entry));

        service.leaveWaitlist(7L, 3L);

        verify(repository).delete(entry);
    }

    @Test
    void ownerCanLeaveNotifiedEntry() {
        WaitlistEntry entry = entry(3L, "NOTIFIED");
        when(repository.findById(7L)).thenReturn(Optional.of(entry));

        service.leaveWaitlist(7L, 3L);

        verify(repository).delete(entry);
    }

    @Test
    void otherCustomerCannotLeaveEntry() {
        when(repository.findById(7L)).thenReturn(
            Optional.of(entry(3L, "WAITING"))
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.leaveWaitlist(7L, 4L));
        verify(repository, never()).delete(any());
    }

    @Test
    void missingEntryCannotBeDeleted() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.leaveWaitlist(7L, 3L));
        verify(repository, never()).delete(any());
    }

    @Test
    void completedEntryCannotBeDeleted() {
        when(repository.findById(7L)).thenReturn(
            Optional.of(entry(3L, "BOOKED"))
        );

        assertThrows(IllegalStateException.class,
            () -> service.leaveWaitlist(7L, 3L));
        verify(repository, never()).delete(any());
    }
}
