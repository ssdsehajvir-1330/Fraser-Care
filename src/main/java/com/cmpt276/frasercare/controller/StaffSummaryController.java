package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.dto.StaffSummaryResponse;
import com.cmpt276.frasercare.service.StaffSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/summary")
public class StaffSummaryController {

    private final StaffSummaryService summaryService;

    public StaffSummaryController(StaffSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    // Today's counts for the dashboard cards
    @GetMapping
    public StaffSummaryResponse getSummary() {
        return summaryService.getTodaySummary();
    }
}