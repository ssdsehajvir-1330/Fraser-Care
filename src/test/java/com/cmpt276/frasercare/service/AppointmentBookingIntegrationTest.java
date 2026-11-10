
package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.Appointment;
import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.repository.PharmacyServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AppointmentBookingIntegrationTest {

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private PharmacyServiceRepository pharmacyServiceRepository;

    @Test
    void cancellingAppointmentAllowsRebooking() {

        List<PharmacyService> services =
                pharmacyServiceRepository.findByActiveTrue();

        assertFalse(services.isEmpty());

        PharmacyService service = services.get(0);
        Long serviceId = service.getServiceId();

        LocalDate date = LocalDate.now(
                ZoneId.of("America/Vancouver")).plusDays(90);

        List<LocalTime> available =
                bookingService.getAvailableStartTimes(serviceId, date);

        assertFalse(available.isEmpty());

        LocalTime start = available.get(0);
        LocalTime end = start.plusMinutes(service.getDurationMinutes());

        Appointment first = new Appointment();
        first.setCustomerId(9100001L);
        first.setServiceId(serviceId);
        first.setAppointmentDate(date);
        first.setStartTime(start);
        first.setEndTime(end);

        Appointment booked = bookingService.bookAppointment(first);

        assertEquals("BOOKED", booked.getStatus());

        assertFalse(bookingService.getAvailableStartTimes(serviceId, date)
                .contains(start));

        Appointment cancelled =
                bookingService.cancelAppointment(
                        booked.getAppointmentId(),
                        booked.getCustomerId()
                );

        assertEquals("CANCELLED", cancelled.getStatus());

        assertTrue(bookingService.getAvailableStartTimes(serviceId, date)
                .contains(start));

        Appointment second = new Appointment();
        second.setCustomerId(9100002L);
        second.setServiceId(serviceId);
        second.setAppointmentDate(date);
        second.setStartTime(start);
        second.setEndTime(end);

        Appointment rebooked = bookingService.bookAppointment(second);

        assertEquals("BOOKED", rebooked.getStatus());
        assertNotEquals(booked.getAppointmentId(), rebooked.getAppointmentId());
    }
}
