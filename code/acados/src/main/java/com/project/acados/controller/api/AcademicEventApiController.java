package com.project.acados.controller.api;

import com.project.acados.domain.entity.AcademicEvent;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.dto.request.AcademicEventRequest;
import com.project.acados.dto.response.AcademicEventResponse;
import com.project.acados.service.AcademicEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Academic Calendar events.
 * Reference: Implement_Plan-AcadOS.md §16, database.md §2.15, class diagram.puml
 */
@RestController
@RequestMapping("/api/v1/academic-events")
@RequiredArgsConstructor
@Tag(name = "Academic Events", description = "Academic Calendar API")
public class AcademicEventApiController {

    private final AcademicEventService academicEventService;

    @GetMapping
    @Operation(summary = "Get all academic events, optionally filtered by event type")
    public ResponseEntity<List<AcademicEventResponse>> getEvents(
            @RequestParam(required = false) AcademicEventType type
    ) {
        List<AcademicEvent> events = (type != null)
                ? academicEventService.getEventsByType(type)
                : academicEventService.getEvents();

        List<AcademicEventResponse> responseList = events.stream()
                .map(AcademicEventResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single academic event by ID")
    public ResponseEntity<AcademicEventResponse> getEvent(@PathVariable Long id) {
        AcademicEvent event = academicEventService.getEvent(id);
        return ResponseEntity.ok(AcademicEventResponse.fromEntity(event));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an academic event (ADMIN only)")
    public ResponseEntity<AcademicEventResponse> createEvent(@Valid @RequestBody AcademicEventRequest request) {
        AcademicEvent created = academicEventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AcademicEventResponse.fromEntity(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an academic event (ADMIN only)")
    public ResponseEntity<AcademicEventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody AcademicEventRequest request
    ) {
        AcademicEvent updated = academicEventService.updateEvent(id, request);
        return ResponseEntity.ok(AcademicEventResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an academic event (ADMIN only)")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        academicEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}

