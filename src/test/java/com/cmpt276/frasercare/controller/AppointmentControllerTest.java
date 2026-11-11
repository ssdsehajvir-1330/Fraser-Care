package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.service.AppointmentBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentControllerTest {

    private final AppointmentBookingService bookingService =
            mock(AppointmentBookingService.class);

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private final AppointmentController controller =
            new AppointmentController(bookingService, jdbc);

    private Authentication customerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "customer@frasercare.ca",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Test
    void customerCanViewOwnAppointments() {
        Authentication customer = customerAuthentication();

        List<Appointment> appointments = List.of(mock(Appointment.class));

        when(jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                "customer@frasercare.ca"
        )).thenReturn(3L);

        when(bookingService.getAppointmentsByCustomerId(3L))
                .thenReturn(appointments);

        ResponseEntity<?> response = controller.getMyAppointments(customer);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(appointments, response.getBody());
        verify(bookingService).getAppointmentsByCustomerId(3L);
    }

    @Test
    void rejectsUnauthenticatedUser() {
        ResponseEntity<?> response = controller.getMyAppointments(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(bookingService);
    }

    @Test
    void rejectsStaffUser() {
        Authentication staff = new UsernamePasswordAuthenticationToken(
                "staff@frasercare.ca",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        ResponseEntity<?> response = controller.getMyAppointments(staff);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(bookingService);
    }

    @Test
    void rejectsMissingCustomerAccount() {
        Authentication customer = customerAuthentication();

        when(jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                "customer@frasercare.ca"
        )).thenThrow(new EmptyResultDataAccessException(1));

        ResponseEntity<?> response = controller.getMyAppointments(customer);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(bookingService);
    }

    @Test
    void customerCanCancelOwnAppointment() {
        Authentication customer = customerAuthentication();

        Appointment cancelled = new Appointment();
        cancelled.setAppointmentId(42L);
        cancelled.setCustomerId(3L);
        cancelled.setStatus("CANCELLED");

        when(jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                "customer@frasercare.ca"
        )).thenReturn(3L);

        when(bookingService.cancelAppointment(42L, 3L))
                .thenReturn(cancelled);

        ResponseEntity<?> response =
                controller.cancelAppointment(42L, customer);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(cancelled, response.getBody());
        assertEquals("CANCELLED",
                ((Appointment) response.getBody()).getStatus());

        verify(bookingService).cancelAppointment(42L, 3L);
    }

    @Test
    void rejectsUnauthenticatedCancellation() {
        ResponseEntity<?> response =
                controller.cancelAppointment(42L, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(jdbc, bookingService);
    }

    @Test
    void rejectsStaffCancellation() {
        Authentication staff = new UsernamePasswordAuthenticationToken(
                "staff@frasercare.ca",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        ResponseEntity<?> response =
                controller.cancelAppointment(42L, staff);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(jdbc, bookingService);
    }

    @Test
    void rejectsCancellationWhenCustomerAccountMissing() {
        Authentication customer = customerAuthentication();

        when(jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                "customer@frasercare.ca"
        )).thenThrow(new EmptyResultDataAccessException(1));

        ResponseEntity<?> response =
                controller.cancelAppointment(42L, customer);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(bookingService);
    }

    @Test
    void rejectsInvalidAppointmentId() {
        Authentication customer = customerAuthentication();

        ResponseEntity<?> response =
                controller.cancelAppointment(0L, customer);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(jdbc, bookingService);
    }

    @Test
    void preventsCustomerFromCancellingAnotherCustomersAppointment() {
        Authentication customer = customerAuthentication();

        when(jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                "customer@frasercare.ca"
        )).thenReturn(3L);

        when(bookingService.cancelAppointment(42L, 3L))
                .thenThrow(new IllegalArgumentException(
                        "Appointment not found."
                ));

        ResponseEntity<?> response =
                controller.cancelAppointment(42L, customer);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(bookingService).cancelAppointment(42L, 3L);
    }

    @Test
    void returnsConflictWhenAppointmentCannotBeCancelled() {
        Authentication customer = customerAuthentication();

        when(jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                "customer@frasercare.ca"
        )).thenReturn(3L);

        when(bookingService.cancelAppointment(42L, 3L))
                .thenThrow(new IllegalStateException(
                        "This appointment is not currently booked."
                ));

        ResponseEntity<?> response =
                controller.cancelAppointment(42L, customer);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
