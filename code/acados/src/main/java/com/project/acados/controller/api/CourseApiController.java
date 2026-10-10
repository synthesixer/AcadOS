package com.project.acados.controller.api;

import com.project.acados.domain.entity.Course;
import com.project.acados.dto.request.CourseRequest;
import com.project.acados.dto.response.CourseResponse;
import com.project.acados.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for Course management.
 * Reference: Implement_Plan-AcadOS.md §7, §16, class diagram.puml
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseApiController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<Page<CourseResponse>> getCourses(
            @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable
    ) {
        Page<CourseResponse> responsePage = courseService.getCourses(pageable)
                .map(CourseResponse::fromEntity);
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id) {
        Course course = courseService.getCourse(id);
        return ResponseEntity.ok(CourseResponse.fromEntity(course));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        Course created = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseResponse.fromEntity(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request
    ) {
        Course updated = courseService.updateCourse(id, request);
        return ResponseEntity.ok(CourseResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}

