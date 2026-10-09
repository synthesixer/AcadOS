package com.project.acados.controller.api;

import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.dto.response.ScheduleResponse;
import com.project.acados.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Schedule management.
 * Coordinates automated timetable generation, review, publication, and draft discard.
 * Reference: Implement_Plan-AcadOS.md §7, §16, activity_schedule_generation.puml
 */
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleApiController {

    private final SchedulingService schedulingService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScheduleResponse>> generateSchedule() {
        schedulingService.generateSchedule();
        List<ScheduleResponse> drafts = schedulingService.getDraftSchedules().stream()
                .map(ScheduleResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(drafts);
    }

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @RequestParam(required = false) ScheduleStatus status
    ) {
        List<Schedule> schedules;
        if (status == ScheduleStatus.DRAFT) {
            schedules = schedulingService.getDraftSchedules();
        } else if (status == ScheduleStatus.PUBLISHED) {
            schedules = schedulingService.getPublishedSchedules();
        } else {
            schedules = schedulingService.getPublishedSchedules();
        }

        List<ScheduleResponse> response = schedules.stream()
                .map(ScheduleResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> publishSchedule() {
        schedulingService.publishSchedule();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/draft")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> discardDraft() {
        schedulingService.discardDraft();
        return ResponseEntity.noContent().build();
    }
}

