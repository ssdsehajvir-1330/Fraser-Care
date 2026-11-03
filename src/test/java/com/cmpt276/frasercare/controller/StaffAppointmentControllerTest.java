package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.dto.StaffAppointmentResponse;
import com.cmpt276.frasercare.service.AppointmentBookingService;
import com.cmpt276.frasercare.service.CustomerNameService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffAppointmentControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 20);

    private StaffAppointmentResponse appointment(long id, long customerId) {
        return new StaffAppointmentResponse(
                id, customerId, null, 1L, "Flu Vaccination", DATE,
                LocalTime.of(9, 0), LocalTime.of(9, 15), "BOOKED");
    }

    @Test
    void addsCustomerNamesToAppointments() {
        AppointmentBookingService bookingService = mock(AppointmentBookingService.class);
        CustomerNameService nameService = mock(CustomerNameService.class);

        when(bookingService.getStaffAppointmentsByDate(DATE))
                .thenReturn(List.of(appointment(1L, 5L), appointment(2L, 6L)));

        when(nameService.getNames(any()))
                .thenReturn(Map.of(5L, "Priya Singh", 6L, "Daniel Lee"));

        List<StaffAppointmentResponse> result =
                new StaffAppointmentController(bookingService, nameService)
                        .getAppointmentsByDate(DATE)
                        .getBody();

        assertNotNull(result);
        assertEquals("Priya Singh", result.get(0).getCustomerName());
        assertEquals("Daniel Lee", result.get(1).getCustomerName());
        assertEquals("Flu Vaccination", result.get(0).getServiceName());
    }

    @Test
    void leavesNameEmptyWhenCustomerNotFound() {
        AppointmentBookingService bookingService = mock(AppointmentBookingService.class);
        CustomerNameService nameService = mock(CustomerNameService.class);

        when(bookingService.getStaffAppointmentsByDate(DATE))
                .thenReturn(List.of(appointment(1L, 99L)));

        when(nameService.getNames(any())).thenReturn(Map.of());

        List<StaffAppointmentResponse> result =
                new StaffAppointmentController(bookingService, nameService)
                        .getAppointmentsByDate(DATE)
                        .getBody();

        assertNotNull(result);
        assertNull(result.get(0).getCustomerName());
        assertEquals(99L, result.get(0).getCustomerId());
    }
}
