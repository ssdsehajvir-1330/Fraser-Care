package com.cmpt276.frasercare.repository;

import com.cmpt276.frasercare.model.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByServiceIdAndAppointmentDateAndStartTimeOrderByJoinedAtAscWaitlistIdAsc(
        Long serviceId,
        LocalDate appointmentDate,
        LocalTime startTime
    );

    // Retrieve all waitlist entries belonging to a customer
    List<WaitlistEntry> findByCustomerIdOrderByJoinedAtDescWaitlistIdDesc(
        Long customerId
    );
}

