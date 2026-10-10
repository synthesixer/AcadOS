package com.project.acados.service.impl;

import com.project.acados.domain.entity.*;
import com.project.acados.dto.request.TeacherAvailabilityToggleRequest;
import com.project.acados.dto.request.TeacherPreferenceRequest;
import com.project.acados.dto.response.CourseResponse;
import com.project.acados.dto.response.TeacherAvailabilityResponse;
import com.project.acados.dto.response.TeacherPreferenceResponse;
import com.project.acados.repository.*;
import com.project.acados.service.TeacherPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeacherPreferenceServiceImpl implements TeacherPreferenceService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherPreferenceRepository teacherPreferenceRepository;
    private final TeacherQualificationRepository teacherQualificationRepository;
    private final CourseRepository courseRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final TimeSlotRepository timeSlotRepository;

    private Teacher resolveTeacher(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบ");
        }
        User user = userRepository.findByUniversityId(userIdentifier)
                .or(() -> userRepository.findByEmail(userIdentifier))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลผู้ใช้"));

        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "บัญชีนี้ไม่มีสิทธิ์ของอาจารย์"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherPreferenceResponse> getPreferences(String userIdentifier) {
        Teacher teacher = resolveTeacher(userIdentifier);
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

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getQualifiedCourses(String userIdentifier) {
        Teacher teacher = resolveTeacher(userIdentifier);
        List<TeacherQualification> qualifications = teacherQualificationRepository.findByTeacherId(teacher.getId());
        return qualifications.stream()
                .map(TeacherQualification::getCourse)
                .map(CourseResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public TeacherPreferenceResponse savePreference(TeacherPreferenceRequest request, String userIdentifier) {
        Teacher teacher = resolveTeacher(userIdentifier);

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
        return new TeacherPreferenceResponse(
                saved.getId(),
                saved.getCourse().getId(),
                saved.getCourse().getCourseCode(),
                saved.getCourse().getTitle(),
                saved.getPriority()
        );
    }

    @Override
    @Transactional
    public void deletePreference(Long id, String userIdentifier) {
        Teacher teacher = resolveTeacher(userIdentifier);
        TeacherPreference preference = teacherPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลความต้องการสอน"));

        if (!preference.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ไม่สามารถลบข้อมูลของอาจารย์ท่านอื่นได้");
        }

        teacherPreferenceRepository.delete(preference);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAvailabilityResponse> getAvailabilities(String userIdentifier) {
        Teacher teacher = resolveTeacher(userIdentifier);
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

    @Override
    @Transactional
    public TeacherAvailabilityResponse updateAvailability(TeacherAvailabilityToggleRequest request, String userIdentifier) {
        Teacher teacher = resolveTeacher(userIdentifier);
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

        return new TeacherAvailabilityResponse(
                saved.getId(),
                slot.getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                saved.isAvailable()
        );
    }
}

