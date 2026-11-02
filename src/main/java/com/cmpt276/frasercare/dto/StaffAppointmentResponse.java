package com.cmpt276.frasercare.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class StaffAppointmentResponse {

    private final Long appointmentId;
    private final Long customerId;
    private final String customerName;
    private final Long serviceId;
    private final String serviceName;
    private final LocalDate appointmentDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String status;

    public StaffAppointmentResponse(
            Long appointmentId,
            Long customerId,
            String customerName,
            Long serviceId,
            String serviceName,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            String status) {

        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }
}
