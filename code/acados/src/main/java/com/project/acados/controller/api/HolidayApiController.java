package com.project.acados.controller.api;

import com.project.acados.dto.response.HolidayResponse;
import com.project.acados.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Public Holiday management.
 * Reference: Implement_Plan-AcadOS.md §7, §16
 */
@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class HolidayApiController {

    private final HolidayService holidayService;

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> getHolidays() {
        List<HolidayResponse> holidays = holidayService.getHolidays().stream()
                .map(HolidayResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(holidays);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HolidayResponse>> syncHolidays(
            @RequestParam(required = false) Integer year
    ) {
        if (year != null) {
            holidayService.syncHolidays(year);
        } else {
            holidayService.syncHolidays();
        }

        List<HolidayResponse> holidays = holidayService.getHolidays().stream()
                .map(HolidayResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(holidays);
    }
}

