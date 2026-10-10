package com.project.acados.service.impl;

import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Registration;
import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.TeacherAvailability;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.dto.request.SectionRequest;
import com.project.acados.dto.response.TeacherAssignmentOptionResponse;
import com.project.acados.exception.BusinessRuleException;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.repository.CourseRepository;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.SectionRepository;
import com.project.acados.repository.TeacherAvailabilityRepository;
import com.project.acados.repository.TeacherQualificationRepository;
import com.project.acados.repository.TeacherRepository;
import com.project.acados.service.NotificationService;
import com.project.acados.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Default SectionService. The checks follow Userflow A12-2 (create), A12-3 (update), and A13 (assign teacher).
 */
@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherQualificationRepository teacherQualificationRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final ScheduleRepository scheduleRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<Section> getSections(Long courseId) {
        return sectionRepository.findAllWithCourse(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Section getSection(Long id) {
        return sectionRepository.findWithCourseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบ Section"));
    }

    @Override
    @Transactional
    public Section createSection(SectionRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบรายวิชา"));
        if (sectionRepository.existsByCourseIdAndSectionNumber(course.getId(), request.sectionNumber())) {
            throw new BusinessRuleException("Section " + request.sectionNumber() + " ของรายวิชานี้มีอยู่แล้ว");
        }
        return sectionRepository.save(Section.builder()
                .course(course)
                .sectionNumber(request.sectionNumber())
                .capacity(request.capacity())
                .status(SectionStatus.ACTIVE)
                .build());
    }

    @Override
    @Transactional
    public Section updateSection(Long id, SectionRequest request) {
        Section section = sectionRepository.findWithCourseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบ Section"));
        if (section.getStatus() != SectionStatus.ACTIVE) {
            throw new BusinessRuleException("Section นี้ถูกยกเลิกแล้ว ไม่สามารถแก้ไขได้");
        }
        if (request.capacity() < registrationRepository.countBySectionId(id)) {
            throw new BusinessRuleException("BR-05: จำนวนที่นั่งน้อยกว่าจำนวนผู้ลงทะเบียน");
        }
        section.setCapacity(request.capacity());
        return section;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentOptionResponse> getTeacherOptions(Long id) {
        Section section = sectionRepository.findWithCourseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบ Section"));
        Long courseId = section.getCourse().getId();
        return teacherRepository.findAll().stream()
                .map(t -> new TeacherAssignmentOptionResponse(
                        t.getId(),
                        t.getFullName(),
                        t.getUser().getUniversityId(),
                        teacherQualificationRepository.existsByTeacherIdAndCourseId(t.getId(), courseId)
                ))
                .toList();
    }

    @Override
    @Transactional
    public Section assignTeacher(Long id, Long teacherId) {
        Section section = sectionRepository.findWithCourseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบ Section"));

        if (section.getStatus() != SectionStatus.ACTIVE) {
            throw new BusinessRuleException("Section นี้ถูกยกเลิกแล้ว ไม่สามารถมอบหมายผู้สอนได้");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบอาจารย์"));

        // BR-06: Teacher Qualification
        if (!teacherQualificationRepository.existsByTeacherIdAndCourseId(teacher.getId(), section.getCourse().getId())) {
            throw new BusinessRuleException("BR-06: อาจารย์ไม่มีคุณสมบัติสอนรายวิชานี้");
        }

        // Schedules must exist and be PUBLISHED
        List<Schedule> schedules = scheduleRepository.findBySectionId(id);
        if (schedules.isEmpty()) {
            throw new BusinessRuleException("Section ยังไม่มีคาบเรียน กรุณาจัดตารางเรียนก่อน");
        }
        boolean hasNonPublished = schedules.stream().anyMatch(s -> s.getStatus() != ScheduleStatus.PUBLISHED);
        if (hasNonPublished) {
            throw new BusinessRuleException("Section มีคาบเรียนที่ยังไม่ได้รับการ Publish (ต้อง Publish ตารางเรียนก่อนมอบหมายผู้สอน)");
        }

        // BR-07: Teacher Availability
        List<TeacherAvailability> unavailabilities = teacherAvailabilityRepository.findByTeacherId(teacher.getId());
        for (Schedule schedule : schedules) {
            boolean unavailable = unavailabilities.stream()
                    .filter(a -> !a.isAvailable())
                    .map(TeacherAvailability::getTimeSlot)
                    .anyMatch(slot -> slot.overlapsWith(schedule.getTimeSlot()));
            if (unavailable) {
                throw new BusinessRuleException("BR-07: อาจารย์ไม่ว่างในช่วงเวลาของคาบเรียน");
            }
        }

        // BR-01: Teacher Schedule Conflict
        for (Schedule schedule : schedules) {
            for (ScheduleStatus status : List.of(ScheduleStatus.PUBLISHED, ScheduleStatus.DRAFT)) {
                List<Schedule> existingSchedules = scheduleRepository.findByTeacherIdAndStatus(teacher.getId(), status);
                for (Schedule existing : existingSchedules) {
                    if (Objects.equals(existing.getSection().getId(), id)) {
                        continue;
                    }
                    if (existing.getTimeSlot().overlapsWith(schedule.getTimeSlot())) {
                        if (status == ScheduleStatus.DRAFT) {
                            throw new BusinessRuleException("DRAFT Timetable Conflict: ตารางร่างของอาจารย์มีคาบสอนชนกัน");
                        }
                        throw new BusinessRuleException("BR-01: อาจารย์มีตารางสอนชนกันในคาบเรียนนี้");
                    }
                }
            }
        }

        // Update schedule teacher
        Teacher previousTeacher = schedules.get(0).getTeacher();
        for (Schedule schedule : schedules) {
            schedule.setTeacher(teacher);
            scheduleRepository.save(schedule);
        }

        // Notifications (Userflow A13)
        notificationService.sendNotification(
                teacher.getUser(),
                NotificationType.SCHEDULE_CHANGED,
                "มอบหมายการสอนรายวิชา",
                "คุณได้รับมอบหมายให้สอนวิชา " + section.getCourse().getCourseCode() + " กลุ่ม " + section.getSectionNumber(),
                NotificationChannel.IN_APP
        );

        if (previousTeacher != null && !Objects.equals(previousTeacher.getId(), teacher.getId())) {
            notificationService.sendNotification(
                    previousTeacher.getUser(),
                    NotificationType.SCHEDULE_CHANGED,
                    "เปลี่ยนแปลงการมอบหมายผู้สอน",
                    "คุณถูกเปลี่ยนผู้สอนในวิชา " + section.getCourse().getCourseCode() + " กลุ่ม " + section.getSectionNumber(),
                    NotificationChannel.IN_APP
            );
        }

        for (Registration reg : registrationRepository.findBySectionId(id)) {
            notificationService.sendNotification(
                    reg.getStudent().getUser(),
                    NotificationType.SCHEDULE_CHANGED,
                    "เปลี่ยนแปลงผู้สอน",
                    "วิชา " + section.getCourse().getCourseCode() + " กลุ่ม " + section.getSectionNumber() + " ได้เปลี่ยนผู้สอนเป็น " + teacher.getFullName(),
                    NotificationChannel.IN_APP
            );
        }

        return section;
    }
}
