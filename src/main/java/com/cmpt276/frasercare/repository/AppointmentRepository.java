package com.cmpt276.frasercare.repository;

import com.cmpt276.frasercare.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            LocalDate appointmentDate,
            String status,
            LocalTime endTime,
            LocalTime startTime
    );

    List<Appointment> findByCustomerIdOrderByAppointmentDateAscStartTimeAsc(Long customerId);
    List<Appointment> findByAppointmentDateAndStatus(
            LocalDate appointmentDate,
            String status
    );

}
