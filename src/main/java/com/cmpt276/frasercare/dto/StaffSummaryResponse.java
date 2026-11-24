package com.cmpt276.frasercare.dto;

import java.time.LocalDate;

// Counts shown at the top of the staff dashboard
public record StaffSummaryResponse(
        LocalDate date,
        long appointmentsToday,
        long pickupsReceived,
        long pickupsReady,
        long waitlistToday) {
}