package com.project.acados.controller.api;

import com.project.acados.dto.request.TeacherAvailabilityToggleRequest;
import com.project.acados.dto.request.TeacherPreferenceRequest;
import com.project.acados.dto.response.CourseResponse;
import com.project.acados.dto.response.TeacherAvailabilityResponse;
import com.project.acados.dto.response.TeacherPreferenceResponse;
import com.project.acados.service.TeacherPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/teacher", "/api/v1"})
@RequiredArgsConstructor
public class TeacherPreferenceApiController {

    private final TeacherPreferenceService teacherPreferenceService;

    // ==========================================
    // Teacher Course Preferences (D21)
    // ==========================================

    @GetMapping("/preferences")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public List<TeacherPreferenceResponse> getPreferences(Authentication auth) {
        return teacherPreferenceService.getPreferences(auth != null ? auth.getName() : null);
    }

    @GetMapping("/qualifications")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public List<CourseResponse> getQualifiedCourses(Authentication auth) {
        return teacherPreferenceService.getQualifiedCourses(auth != null ? auth.getName() : null);
    }

    @PostMapping("/preferences")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherPreferenceResponse> savePreference(
            @Valid @RequestBody TeacherPreferenceRequest request,
            Authentication auth
    ) {
        TeacherPreferenceResponse response = teacherPreferenceService.savePreference(request, auth != null ? auth.getName() : null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/preferences/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deletePreference(@PathVariable Long id, Authentication auth) {
        teacherPreferenceService.deletePreference(id, auth != null ? auth.getName() : null);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Teacher Availability (D22, BR-07)
    // Records unavailable time slots (is_available = false)
    // ==========================================

    @GetMapping("/availabilities")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public List<TeacherAvailabilityResponse> getAvailabilities(Authentication auth) {
        return teacherPreferenceService.getAvailabilities(auth != null ? auth.getName() : null);
    }

    @PutMapping("/availabilities")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherAvailabilityResponse> updateAvailability(
            @Valid @RequestBody TeacherAvailabilityToggleRequest request,
            Authentication auth
    ) {
        TeacherAvailabilityResponse response = teacherPreferenceService.updateAvailability(request, auth != null ? auth.getName() : null);
        return ResponseEntity.ok(response);
    }
}
