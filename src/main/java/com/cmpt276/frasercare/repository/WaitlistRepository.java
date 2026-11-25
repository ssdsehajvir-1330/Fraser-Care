package com.cmpt276.frasercare.repository;

import com.cmpt276.frasercare.model.WaitlistEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByServiceIdAndAppointmentDateAndStartTimeOrderByJoinedAtAscWaitlistIdAsc(
        Long serviceId,
        LocalDate appointmentDate,
        LocalTime startTime
    );

    List<WaitlistEntry> findByCustomerIdOrderByJoinedAtDescWaitlistIdDesc(
        Long customerId
    );

    List<WaitlistEntry> findByAppointmentDateOrderByServiceIdAscStartTimeAscJoinedAtAscWaitlistIdAsc(
        LocalDate appointmentDate
    );

    // Count customers currently waiting for a specific appointment slot
    long countByServiceIdAndAppointmentDateAndStartTimeAndStatus(
        Long serviceId,
        LocalDate appointmentDate,
        LocalTime startTime,
        String status
    );

    // Lock individual queue during notification
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w FROM WaitlistEntry w
        WHERE w.serviceId = :serviceId
          AND w.appointmentDate = :appointmentDate
          AND w.startTime = :startTime
        ORDER BY w.joinedAt ASC, w.waitlistId ASC
        """)
    List<WaitlistEntry> findQueueForUpdate(
        @Param("serviceId") Long serviceId,
        @Param("appointmentDate") LocalDate appointmentDate,
        @Param("startTime") LocalTime startTime
    );

    // Lock waitlist entries across all services for date so overlapping notification approvals can be coordinated
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w FROM WaitlistEntry w
        WHERE w.appointmentDate = :appointmentDate
        ORDER BY w.waitlistId ASC
        """)
    List<WaitlistEntry> findAllForDateForUpdate(
        @Param("appointmentDate") LocalDate appointmentDate
    );
}