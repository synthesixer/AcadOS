package com.project.acados.controller.api;

import com.project.acados.domain.entity.TeacherSwapRequest;
import com.project.acados.dto.request.SwapCreateRequest;
import com.project.acados.dto.request.SwapResponseRequest;
import com.project.acados.dto.response.SwapInboxResponse;
import com.project.acados.service.TeacherSwapQueryService;
import com.project.acados.service.TeacherSwapService;
import com.project.acados.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/teacher-swaps")
public class TeacherSwapApiController {
    private final TeacherSwapService teacherSwapService;
    private final TeacherSwapQueryService queryService;
    private final UserService userService;

    public TeacherSwapApiController(
            TeacherSwapService teacherSwapService,
            TeacherSwapQueryService queryService,
            UserService userService
    ) {
        this.teacherSwapService = teacherSwapService;
        this.queryService = queryService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public SwapInboxResponse getMyRequests(Authentication authentication) {
        Long teacherId = userService.getTeacherIdByUniversityId(authentication.getName());
        return queryService.getRequestsForTeacher(teacherId);
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<SwapResponse> create(
            @Valid @RequestBody SwapCreateRequest request,
            Authentication authentication
    ) {
        Long requestingTeacherId = userService.getTeacherIdByUniversityId(authentication.getName());
        TeacherSwapRequest saved = teacherSwapService.createSwapRequest(
                requestingTeacherId,
                request.requestingScheduleId(),
                request.targetTeacherId(),
                request.targetScheduleId()
        );
        return ResponseEntity.created(URI.create("/api/v1/teacher-swaps/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PutMapping("/{id}/respond")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> respond(
            @PathVariable Long id,
            @Valid @RequestBody SwapResponseRequest request,
            Authentication authentication
    ) {
        Long respondingTeacherId = userService.getTeacherIdByUniversityId(authentication.getName());
        teacherSwapService.respondSwap(id, respondingTeacherId, request.accept());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        teacherSwapService.approveSwap(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        teacherSwapService.rejectSwap(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Authentication authentication) {
        Long requestingTeacherId = userService.getTeacherIdByUniversityId(authentication.getName());
        teacherSwapService.cancelSwap(id, requestingTeacherId);
        return ResponseEntity.ok().build();
    }

    private SwapResponse toResponse(TeacherSwapRequest request) {
        return new SwapResponse(
                request.getId(),
                request.getRequestingTeacher().getId(),
                request.getRequestingSchedule() == null ? null : request.getRequestingSchedule().getId(),
                request.getTargetTeacher().getId(),
                request.getTargetSchedule() == null ? null : request.getTargetSchedule().getId(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getRespondedAt(),
                request.getReviewedAt()
        );
    }
}
