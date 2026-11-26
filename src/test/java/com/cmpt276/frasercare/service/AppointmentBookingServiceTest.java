package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.repository.AppointmentRepository;
import com.cmpt276.frasercare.repository.PharmacyServiceRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentBookingServiceTest {

    @Test
    void cancellingAppointmentMakesTimeAvailableAgain() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        PharmacyService pharmacyService = mock(PharmacyService.class);
        when(pharmacyRepo.findById(1L)).thenReturn(Optional.of(pharmacyService));
        when(pharmacyService.isActive()).thenReturn(true);
        when(pharmacyService.getDurationMinutes()).thenReturn(15);

        LocalDate date = LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(2);

        Appointment appointment = new Appointment();
        appointment.setCustomerId(5L);
        appointment.setAppointmentDate(date);
        appointment.setStatus("BOOKED");
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 15));

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(appointment)).thenReturn(appointment);

        when(appointmentRepo.findByAppointmentDateAndStatus(date, "BOOKED"))
                .thenAnswer(call -> {
                    if ("BOOKED".equals(appointment.getStatus())) {
                        return List.of(appointment);
                    }
                    return List.of();
                });

        assertFalse(bookingService.getAvailableStartTimes(1L, date)
                .contains(LocalTime.of(10, 0)));

        Appointment cancelled = bookingService.cancelAppointment(1L, 5L);

        assertEquals("CANCELLED", cancelled.getStatus());

        assertTrue(bookingService.getAvailableStartTimes(1L, date)
                .contains(LocalTime.of(10, 0)));

        verify(appointmentRepo).save(appointment);
    }

    @Test
    void cannotCancelAlreadyCancelledAppointment() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = new Appointment();
        appointment.setCustomerId(5L);
        appointment.setStatus("CANCELLED");

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> bookingService.cancelAppointment(1L, 5L));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotCancelMissingAppointment() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        when(appointmentRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.cancelAppointment(999L, 5L));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotCancelAnotherCustomersAppointment() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = new Appointment();
        appointment.setCustomerId(6L);
        appointment.setAppointmentDate(
                LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(2)
        );
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 15));
        appointment.setStatus("BOOKED");

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.cancelAppointment(1L, 5L));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotCancelPastAppointment() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = new Appointment();
        appointment.setCustomerId(5L);
        appointment.setAppointmentDate(
                LocalDate.now(ZoneId.of("America/Vancouver")).minusDays(1)
        );
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 15));
        appointment.setStatus("BOOKED");

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> bookingService.cancelAppointment(1L, 5L));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotBookBeforeOpeningTime() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        PharmacyService pharmacyService = mock(PharmacyService.class);

        when(pharmacyRepo.findById(1L)).thenReturn(Optional.of(pharmacyService));
        when(pharmacyService.isActive()).thenReturn(true);
        when(pharmacyService.getDurationMinutes()).thenReturn(15);

        Appointment appointment = new Appointment();
        appointment.setCustomerId(1L);
        appointment.setServiceId(1L);
        appointment.setAppointmentDate(
                LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(2)
        );
        appointment.setStartTime(LocalTime.of(8, 0));
        appointment.setEndTime(LocalTime.of(8, 15));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookAppointment(appointment));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotBookAppointmentWithExtraSeconds() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        PharmacyService pharmacyService = mock(PharmacyService.class);

        when(pharmacyRepo.findById(1L)).thenReturn(Optional.of(pharmacyService));
        when(pharmacyService.isActive()).thenReturn(true);
        when(pharmacyService.getDurationMinutes()).thenReturn(15);

        Appointment appointment = new Appointment();
        appointment.setCustomerId(1L);
        appointment.setServiceId(1L);
        appointment.setAppointmentDate(
                LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(2)
        );
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 15, 30));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookAppointment(appointment));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotBookWithExistingAppointmentId() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(99L);
        appointment.setCustomerId(1L);
        appointment.setServiceId(1L);
        appointment.setAppointmentDate(
                LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(2)
        );
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 15));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.bookAppointment(appointment));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    private Appointment createPastAppointment() {
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(1L);
        appointment.setCustomerId(5L);
        appointment.setServiceId(1L);
        appointment.setAppointmentDate(
                LocalDate.now(ZoneId.of("America/Vancouver")).minusDays(1)
        );
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 15));
        appointment.setStatus("BOOKED");

        return appointment;
    }

    @Test
    void canMarkPastAppointmentAsCompleted() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = createPastAppointment();

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(appointment)).thenReturn(appointment);

        Appointment updated =
                bookingService.updateAppointmentStatus(1L, "COMPLETED");

        assertEquals("COMPLETED", updated.getStatus());

        verify(appointmentRepo).save(appointment);
    }

    @Test
    void canMarkPastAppointmentAsNoShow() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = createPastAppointment();

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(appointment)).thenReturn(appointment);

        Appointment updated =
                bookingService.updateAppointmentStatus(1L, "NO_SHOW");

        assertEquals("NO_SHOW", updated.getStatus());

        verify(appointmentRepo).save(appointment);
    }

    @Test
    void cannotUpdateFutureAppointmentStatus() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = createPastAppointment();
        appointment.setAppointmentDate(
                LocalDate.now(ZoneId.of("America/Vancouver")).plusDays(1)
        );

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> bookingService.updateAppointmentStatus(1L, "COMPLETED"));

        assertEquals("BOOKED", appointment.getStatus());

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotUpdateOngoingAppointmentStatus() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = createPastAppointment();
        appointment.setAppointmentDate(
                LocalDate.now(ZoneId.of("America/Vancouver"))
        );
        appointment.setStartTime(LocalTime.MIN);
        appointment.setEndTime(LocalTime.MAX);

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> bookingService.updateAppointmentStatus(1L, "NO_SHOW"));

        assertEquals("BOOKED", appointment.getStatus());

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotUpdateCancelledAppointmentStatus() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = createPastAppointment();
        appointment.setStatus("CANCELLED");

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> bookingService.updateAppointmentStatus(1L, "COMPLETED"));

        assertEquals("CANCELLED", appointment.getStatus());

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotUpdateAlreadyCompletedOrNoShowAppointment() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        Appointment appointment = createPastAppointment();
        appointment.setStatus("COMPLETED");

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> bookingService.updateAppointmentStatus(1L, "NO_SHOW"));

        assertEquals("COMPLETED", appointment.getStatus());

        appointment.setStatus("NO_SHOW");

        assertThrows(IllegalStateException.class,
                () -> bookingService.updateAppointmentStatus(1L, "COMPLETED"));

        assertEquals("NO_SHOW", appointment.getStatus());

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotSetInvalidAppointmentStatus() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateAppointmentStatus(1L, "INVALID"));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateAppointmentStatus(1L, "CANCELLED"));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateAppointmentStatus(1L, null));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotUpdateMissingAppointment() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        when(appointmentRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateAppointmentStatus(999L, "COMPLETED"));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }

    @Test
    void cannotUpdateInvalidAppointmentId() {

        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        PharmacyServiceRepository pharmacyRepo = mock(PharmacyServiceRepository.class);

        AppointmentBookingService bookingService =
                new AppointmentBookingService(appointmentRepo, pharmacyRepo);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateAppointmentStatus(0L, "COMPLETED"));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateAppointmentStatus(-1L, "COMPLETED"));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateAppointmentStatus(null, "COMPLETED"));

        verify(appointmentRepo, never()).save(any(Appointment.class));
    }
}
