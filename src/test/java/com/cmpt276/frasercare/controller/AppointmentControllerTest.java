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

    @Test
    void customerCanViewOwnAppointments() {
        Authentication customer = new UsernamePasswordAuthenticationToken(
                "customer@frasercare.ca",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

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
        Authentication customer = new UsernamePasswordAuthenticationToken(
                "missing@frasercare.ca",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(jdbc.queryForObject(
                "SELECT user_id FROM auth_users WHERE username = ?",
                Long.class,
                "missing@frasercare.ca"
        )).thenThrow(new EmptyResultDataAccessException(1));

        ResponseEntity<?> response = controller.getMyAppointments(customer);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(bookingService);
    }
}
