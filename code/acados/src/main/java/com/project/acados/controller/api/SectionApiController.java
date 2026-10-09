package com.project.acados.controller.api;

import com.project.acados.domain.entity.Section;
import com.project.acados.dto.request.SectionRequest;
import com.project.acados.dto.response.SectionResponse;
import com.project.acados.mapper.SectionMapper;
import com.project.acados.service.SectionCancellationService;
import com.project.acados.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST API for section management. Sections are cancelled, never deleted.
 * Reference: Implement_Plan-AcadOS.md §16, Userflow A08 and A12-1 to A12-3
 */
@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
@Tag(name = "Sections", description = "Section management and cancellation")
public class SectionApiController {

    private final SectionService sectionService;
    private final SectionCancellationService sectionCancellationService;
    private final SectionMapper sectionMapper;

    @GetMapping
    @Operation(summary = "List sections, optionally of one course")
    public List<SectionResponse> getSections(@RequestParam(required = false) Long courseId) {
        return sectionMapper.toResponseList(sectionService.getSections(courseId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one section")
    public SectionResponse getSection(@PathVariable Long id) {
        return sectionMapper.toResponse(sectionService.getSection(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a section of a course")
    public ResponseEntity<SectionResponse> createSection(
            @Validated({Default.class, SectionRequest.OnCreate.class}) @RequestBody SectionRequest request) {
        Section section = sectionService.createSection(request);
        return ResponseEntity
                .created(URI.create("/api/v1/sections/" + section.getId()))
                .body(sectionMapper.toResponse(section));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change the capacity of a section")
    public SectionResponse updateSection(@PathVariable Long id, @Valid @RequestBody SectionRequest request) {
        return sectionMapper.toResponse(sectionService.updateSection(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a section, delete its registrations and release its periods")
    public SectionResponse cancelSection(@PathVariable Long id) {
        sectionCancellationService.cancelSection(id);
        return sectionMapper.toResponse(sectionService.getSection(id));
    }
}
