package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.PickupRequest;
import com.cmpt276.frasercare.repository.PickupRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class PickupServiceTest {

    private PickupRequestRepository pickupRepository;
    private PickupService pickupService;

    @BeforeEach
    void setUp() {
        pickupRepository = Mockito.mock(PickupRequestRepository.class);
        pickupService = new PickupService(pickupRepository);

        // Return whatever gets saved
        Mockito.when(pickupRepository.save(any(PickupRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Helper to make a pickup with a given status
    private PickupRequest pickupWithStatus(String status) {
        PickupRequest pickup = new PickupRequest();
        pickup.setPickupId(1L);
        pickup.setCustomerId(5L);
        pickup.setItemName("Refill: Amoxicillin");
        pickup.setStatus(status);
        return pickup;
    }

    // Tomorrow at a set hour, so tests don't depend on the current time
    private LocalDateTime tomorrowAt(int hour) {
        return LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0)
                .withSecond(0).withNano(0);
    }

    // New request starts as RECEIVED and the item name is trimmed
    @Test
    void createRequest_validDetails_savedAsReceived() {
        PickupRequest result =
                pickupService.createRequest(5L, "  Refill: Amoxicillin  ", "After 3pm");

        assertEquals("RECEIVED", result.getStatus());
        assertEquals("Refill: Amoxicillin", result.getItemName());
        assertEquals(5L, result.getCustomerId());
    }

    // Blank item name is rejected and nothing is saved
    @Test
    void createRequest_blankItem_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> pickupService.createRequest(5L, "   ", null));

        Mockito.verify(pickupRepository, Mockito.never()).save(any());
    }

    // Notes over 255 characters are rejected
    @Test
    void createRequest_notesTooLong_throwsException() {
        String longNotes = "a".repeat(256);

        assertThrows(IllegalArgumentException.class,
                () -> pickupService.createRequest(5L, "Refill", longNotes));
    }

    // RECEIVED can move to READY
    @Test
    void updateStatus_receivedToReady_updates() {
        Mockito.when(pickupRepository.findById(1L))
                .thenReturn(Optional.of(pickupWithStatus("RECEIVED")));

        PickupRequest result = pickupService.updateStatus(1L, "READY");

        assertEquals("READY", result.getStatus());
    }

    // READY can move to PICKED_UP (lowercase input still works)
    @Test
    void updateStatus_readyToPickedUp_updates() {
        Mockito.when(pickupRepository.findById(1L))
                .thenReturn(Optional.of(pickupWithStatus("READY")));

        PickupRequest result = pickupService.updateStatus(1L, "picked_up");

        assertEquals("PICKED_UP", result.getStatus());
    }

    // Can't skip from RECEIVED straight to PICKED_UP
    @Test
    void updateStatus_skipStep_throwsException() {
        Mockito.when(pickupRepository.findById(1L))
                .thenReturn(Optional.of(pickupWithStatus("RECEIVED")));

        assertThrows(IllegalStateException.class,
                () -> pickupService.updateStatus(1L, "PICKED_UP"));
    }

    // Can't move a pickup backwards
    @Test
    void updateStatus_goBackwards_throwsException() {
        Mockito.when(pickupRepository.findById(1L))
                .thenReturn(Optional.of(pickupWithStatus("READY")));

        assertThrows(IllegalStateException.class,
                () -> pickupService.updateStatus(1L, "RECEIVED"));
    }

    // Once picked up, the status can't change
    @Test
    void updateStatus_alreadyPickedUp_throwsException() {
        Mockito.when(pickupRepository.findById(1L))
                .thenReturn(Optional.of(pickupWithStatus("PICKED_UP")));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pickupService.updateStatus(1L, "READY"));

        assertEquals("This pickup has already been picked up.", exception.getMessage());
    }

    // Unknown pickup ID gives an error
    @Test
    void updateStatus_pickupNotFound_throwsException() {
        Mockito.when(pickupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> pickupService.updateStatus(99L, "READY"));
    }

    // Staff filter only accepts real statuses
    @Test
    void getAllPickups_invalidStatus_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> pickupService.getAllPickups("LOST"));
    }

    // Staff can set a ready time in the future
    @Test
    void setReadyTime_futureTime_saves() {
        Mockito.when(pickupRepository.findById(1L))
                .thenReturn(Optional.of(pickupWithStatus("RECEIVED")));

        LocalDateTime readyBy = tomorrowAt(10);
        PickupRequest result = pickupService.setReadyTime(1L, readyBy);

        assertEquals(readyBy, result.getReadyBy());
    }

    // A time in the past is rejected and nothing is saved
    @Test
    void setReadyTime_pastTime_throwsException() {
        LocalDateTime readyBy = LocalDateTime.now().minusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> pickupService.setReadyTime(1L, readyBy));

        Mockito.verify(pickupRepository, Mockito.never()).save(any());
    }

    // Missing time is rejected
    @Test
    void setReadyTime_nullTime_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> pickupService.setReadyTime(1L, null));
    }

    // Can't set a ready time once it's been picked up
    @Test
    void setReadyTime_alreadyPickedUp_throwsException() {
        Mockito.when(pickupRepository.findById(1L))
                .thenReturn(Optional.of(pickupWithStatus("PICKED_UP")));

        LocalDateTime readyBy = tomorrowAt(10);

        assertThrows(IllegalStateException.class,
                () -> pickupService.setReadyTime(1L, readyBy));
    }

    // A time after closing is rejected
    @Test
    void setReadyTime_afterHours_throwsException() {
        LocalDateTime readyBy = tomorrowAt(20);

        assertThrows(IllegalArgumentException.class,
                () -> pickupService.setReadyTime(1L, readyBy));

        Mockito.verify(pickupRepository, Mockito.never()).save(any());
    }

    // Closing time itself is still allowed
    @Test
    void setReadyTime_atClosingTime_saves() {
        Mockito.when(pickupRepository.findById(1L))
                .thenReturn(Optional.of(pickupWithStatus("RECEIVED")));

        LocalDateTime readyBy = tomorrowAt(17);
        PickupRequest result = pickupService.setReadyTime(1L, readyBy);

        assertEquals(readyBy, result.getReadyBy());
    }
} 