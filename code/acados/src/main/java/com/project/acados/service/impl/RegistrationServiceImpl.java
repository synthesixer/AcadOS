package com.project.acados.service.impl;

import com.project.acados.domain.entity.Registration;
import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Student;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.repository.UserRepository;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.exception.BusinessRuleException;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.repository.AcademicEventRepository;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.SectionRepository;
import com.project.acados.repository.StudentRepository;
import com.project.acados.service.NotificationService;
import com.project.acados.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Default RegistrationService. The checks run in the order of Sequence Diagram 04.
 */
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final AcademicEventRepository academicEventRepository;
    private final SectionRepository sectionRepository;
    private final RegistrationRepository registrationRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Registration register(String universityId, Long sectionId) {
        Student student = findStudent(universityId);
        requireRegistrationPeriod();

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบ Section ที่เลือก"));
        if (section.getStatus() != SectionStatus.ACTIVE) {
            throw new BusinessRuleException("Section นี้ถูกยกเลิกแล้ว ไม่สามารถลงทะเบียนได้");
        }
        if (checkDuplicate(student.getId(), section.getCourse().getId())) {
            throw new BusinessRuleException("BR-04: ลงทะเบียนวิชานี้ไปแล้ว");
        }
        if (registrationRepository.countBySectionId(sectionId) >= section.getCapacity()) {
            throw new BusinessRuleException("BR-05: Section นี้เต็มแล้ว");
        }
        if (checkConflict(student.getId(), sectionId)) {
            notificationService.sendNotificationInNewTransaction(student.getUser(),
                    NotificationType.CONFLICT_DETECTED,
                    "ลงทะเบียนไม่สำเร็จ: เวลาเรียนชนกัน",
                    sectionLabel(section) + " มีเวลาเรียนชนกับ Section ที่ลงทะเบียนไว้แล้ว",
                    NotificationChannel.IN_APP);
            throw new BusinessRuleException("BR-03: เวลาเรียนชนกับ Section ที่ลงทะเบียนไว้แล้ว");
        }

        Registration registration = registrationRepository.save(Registration.builder()
                .student(student)
                .section(section)
                .course(section.getCourse())
                .build());

        notificationService.sendNotification(student.getUser(),
                NotificationType.REGISTRATION_SUCCESS,
                "ลงทะเบียนสำเร็จ",
                "คุณลงทะเบียน " + sectionLabel(section) + " เรียบร้อยแล้ว",
                NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        return registration;
    }

    @Override
    @Transactional
    public void withdraw(String universityId, Long registrationId) {
        Student student = findStudent(universityId);
        requireRegistrationPeriod();

        Registration registration = registrationRepository.findByIdAndStudentId(registrationId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบการลงทะเบียน"));
        String label = sectionLabel(registration.getSection());
        registrationRepository.delete(registration);

        notificationService.sendNotification(student.getUser(),
                NotificationType.REGISTRATION_WITHDRAWN,
                "ถอนรายวิชาสำเร็จ",
                "คุณถอน " + label + " เรียบร้อยแล้ว",
                NotificationChannel.IN_APP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Registration> getRegistrations(String universityId, Long sectionId) {
        User user = userRepository.findByUniversityId(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบข้อมูลผู้ใช้"));
        if (user.getRole() == UserRole.ADMIN) {
            return registrationRepository.findAllWithDetails(null, sectionId);
        }
        Student student = findStudent(universityId);
        return registrationRepository.findAllWithDetails(student.getId(), sectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkDuplicate(Long studentId, Long courseId) {
        return registrationRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkConflict(Long studentId, Long sectionId) {
        List<Schedule> newPeriods = publishedPeriods(sectionId);
        if (newPeriods.isEmpty()) {
            return false;
        }
        List<Schedule> registeredPeriods = new ArrayList<>();
        for (Registration registration : registrationRepository.findByStudentId(studentId)) {
            registeredPeriods.addAll(publishedPeriods(registration.getSection().getId()));
        }
        for (Schedule newPeriod : newPeriods) {
            for (Schedule registeredPeriod : registeredPeriods) {
                if (newPeriod.getTimeSlot().overlapsWith(registeredPeriod.getTimeSlot())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Student findStudent(String universityId) {
        return studentRepository.findByUserUniversityId(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบข้อมูลนักศึกษา"));
    }

    /**
     * BR-09: registering and withdrawing are allowed only inside a REGISTRATION_PERIOD event.
     */
    private void requireRegistrationPeriod() {
        boolean insidePeriod = !academicEventRepository
                .findActiveEventsByTypeAndDate(AcademicEventType.REGISTRATION_PERIOD, LocalDate.now())
                .isEmpty();
        if (!insidePeriod) {
            throw new BusinessRuleException("BR-09: ไม่อยู่ในช่วงเวลาลงทะเบียน");
        }
    }

    private List<Schedule> publishedPeriods(Long sectionId) {
        return scheduleRepository.findBySectionId(sectionId).stream()
                .filter(schedule -> schedule.getStatus() == ScheduleStatus.PUBLISHED)
                .toList();
    }

    private String sectionLabel(Section section) {
        return section.getCourse().getCourseCode() + " Section " + section.getSectionNumber();
    }
}
