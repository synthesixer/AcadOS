package com.project.acados.controller.api;

import com.project.acados.domain.entity.*;
import com.project.acados.dto.request.TeacherAvailabilityToggleRequest;
import com.project.acados.dto.request.TeacherPreferenceRequest;
import com.project.acados.dto.response.CourseResponse;
import com.project.acados.dto.response.TeacherAvailabilityResponse;
import com.project.acados.dto.response.TeacherPreferenceResponse;
import com.project.acados.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/api/v1/teacher", "/api/v1"})
@RequiredArgsConstructor
public class TeacherPreferenceApiController {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherPreferenceRepository teacherPreferenceRepository;
    private final TeacherQualificationRepository teacherQualificationRepository;
    private final CourseRepository courseRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final TimeSlotRepository timeSlotRepository;

    private Teacher getCurrentTeacher(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบ");
        }
        User user = userRepository.findByUniversityId(auth.getName())
                .or(() -> userRepository.findByEmail(auth.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลผู้ใช้"));

        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "บัญชีนี้ไม่มีสิทธิ์ของอาจารย์"));
    }

    // ==========================================
    // Teacher Course Preferences (D21)
    // ==========================================

    @GetMapping("/preferences")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<TeacherPreferenceResponse> getPreferences(Authentication auth) {
        Teacher teacher = getCurrentTeacher(auth);
        return teacherPreferenceRepository.findByTeacherIdOrderByPriorityAsc(teacher.getId()).stream()
                .map(p -> new TeacherPreferenceResponse(
                        p.getId(),
                        p.getCourse().getId(),
                        p.getCourse().getCourseCode(),
                        p.getCourse().getTitle(),
                        p.getPriority()
                ))
                .toList();
    }

    @GetMapping("/qualifications")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<CourseResponse> getQualifiedCourses(Authentication auth) {
        Teacher teacher = getCurrentTeacher(auth);
        List<TeacherQualification> qualifications = teacherQualificationRepository.findByTeacherId(teacher.getId());
        return qualifications.stream()
                .map(TeacherQualification::getCourse)
                .map(CourseResponse::fromEntity)
                .toList();
    }

    @PostMapping("/preferences")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public ResponseEntity<TeacherPreferenceResponse> savePreference(
            @Valid @RequestBody TeacherPreferenceRequest request,
            Authentication auth
    ) {
        Teacher teacher = getCurrentTeacher(auth);

        // Verify qualification (BR-06)
        if (!teacherQualificationRepository.existsByTeacherIdAndCourseId(teacher.getId(), request.courseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ท่านยังไม่มีคุณสมบัติในการสอนรายวิชานี้ (BR-06)");
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบรายวิชาที่ระบุ"));

        List<TeacherPreference> existing = teacherPreferenceRepository.findByTeacherId(teacher.getId());
        TeacherPreference target = existing.stream()
                .filter(p -> p.getCourse().getId().equals(request.courseId()))
                .findFirst()
                .orElse(null);

        if (target != null) {
            target.setPriority(request.priority());
        } else {
            target = TeacherPreference.builder()
                    .teacher(teacher)
                    .course(course)
                    .priority(request.priority())
                    .build();
        }

        TeacherPreference saved = teacherPreferenceRepository.save(target);
        return ResponseEntity.ok(new TeacherPreferenceResponse(
                saved.getId(),
                saved.getCourse().getId(),
                saved.getCourse().getCourseCode(),
                saved.getCourse().getTitle(),
                saved.getPriority()
        ));
    }

    @DeleteMapping("/preferences/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public ResponseEntity<Void> deletePreference(@PathVariable Long id, Authentication auth) {
        Teacher teacher = getCurrentTeacher(auth);
        TeacherPreference preference = teacherPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลความต้องการสอน"));

        if (!preference.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ไม่สามารถลบข้อมูลของอาจารย์ท่านอื่นได้");
        }

        teacherPreferenceRepository.delete(preference);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Teacher Availability (D22, BR-07)
    // Records unavailable time slots (is_available = false)
    // ==========================================

    @GetMapping("/availabilities")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<TeacherAvailabilityResponse> getAvailabilities(Authentication auth) {
        Teacher teacher = getCurrentTeacher(auth);
        List<TimeSlot> allSlots = timeSlotRepository.findAll().stream()
                .sorted(Comparator.comparing(TimeSlot::getDayOfWeek)
                        .thenComparing(TimeSlot::getStartTime))
                .toList();

        return allSlots.stream().map(slot -> {
            Optional<TeacherAvailability> availOpt = teacherAvailabilityRepository
                    .findByTeacherIdAndTimeSlotId(teacher.getId(), slot.getId());
            boolean isAvail = availOpt.map(TeacherAvailability::isAvailable).orElse(true);
            Long recordId = availOpt.map(TeacherAvailability::getId).orElse(null);
            return new TeacherAvailabilityResponse(
                    recordId,
                    slot.getId(),
                    slot.getDayOfWeek(),
                    slot.getStartTime(),
                    slot.getEndTime(),
                    isAvail
            );
        }).toList();
    }

    @PutMapping("/availabilities")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public ResponseEntity<TeacherAvailabilityResponse> updateAvailability(
            @Valid @RequestBody TeacherAvailabilityToggleRequest request,
            Authentication auth
    ) {
        Teacher teacher = getCurrentTeacher(auth);
        TimeSlot slot = timeSlotRepository.findById(request.timeSlotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบรหัสช่วงเวลาที่ระบุ"));

        TeacherAvailability target = teacherAvailabilityRepository
                .findByTeacherIdAndTimeSlotId(teacher.getId(), slot.getId())
                .orElseGet(() -> TeacherAvailability.builder()
                        .teacher(teacher)
                        .timeSlot(slot)
                        .isAvailable(true)
                        .build());

        target.setAvailable(request.isAvailable());
        TeacherAvailability saved = teacherAvailabilityRepository.save(target);

        return ResponseEntity.ok(new TeacherAvailabilityResponse(
                saved.getId(),
                slot.getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                saved.isAvailable()
        ));
    }
}
