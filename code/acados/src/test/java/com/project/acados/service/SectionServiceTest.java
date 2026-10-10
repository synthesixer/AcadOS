package com.project.acados.service;

import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Registration;
import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Student;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.TeacherAvailability;
import com.project.acados.domain.entity.TimeSlot;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.SectionRequest;
import com.project.acados.dto.response.TeacherAssignmentOptionResponse;
import com.project.acados.exception.BusinessRuleException;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.repository.*;
import com.project.acados.service.impl.SectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherQualificationRepository teacherQualificationRepository;

    @Mock
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SectionServiceImpl sectionService;

    private Course course;
    private Section section;

    @BeforeEach
    void setUp() {
        course = Course.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(3).build();
        section = Section.builder().id(100L).course(course).sectionNumber(1).capacity(40).build();
    }

    @Test
    void getSections_returnsSectionsOfTheCourse() {
        when(sectionRepository.findAllWithCourse(5L)).thenReturn(List.of(section));

        assertThat(sectionService.getSections(5L)).containsExactly(section);
    }

    @Test
    void getSections_whenCourseIdNull_returnsAllSections() {
        when(sectionRepository.findAllWithCourse(null)).thenReturn(List.of(section));

        assertThat(sectionService.getSections(null)).containsExactly(section);
    }

    @Test
    void getSection_whenMissing_throwsResourceNotFoundException() {
        when(sectionRepository.findWithCourseById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.getSection(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createSection_savesActiveSectionOfTheCourse() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(sectionRepository.existsByCourseIdAndSectionNumber(5L, 2)).thenReturn(false);
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Section created = sectionService.createSection(new SectionRequest(5L, 2, 30));

        assertThat(created.getCourse()).isSameAs(course);
        assertThat(created.getSectionNumber()).isEqualTo(2);
        assertThat(created.getCapacity()).isEqualTo(30);
        assertThat(created.getStatus()).isEqualTo(SectionStatus.ACTIVE);
    }

    @Test
    void createSection_whenCourseMissing_throwsResourceNotFoundException() {
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.createSection(new SectionRequest(9L, 1, 30)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_whenSectionNumberExistsInCourse_throwsBusinessRuleException() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(sectionRepository.existsByCourseIdAndSectionNumber(5L, 1)).thenReturn(true);

        assertThatThrownBy(() -> sectionService.createSection(new SectionRequest(5L, 1, 30)))
                .isInstanceOf(BusinessRuleException.class);

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateSection_changesCapacityOnly() {
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(registrationRepository.countBySectionId(100L)).thenReturn(10L);

        Section updated = sectionService.updateSection(100L, new SectionRequest(99L, 7, 25));

        assertThat(updated.getCapacity()).isEqualTo(25);
        assertThat(updated.getSectionNumber()).isEqualTo(1);
        assertThat(updated.getCourse()).isSameAs(course);
    }

    @Test
    void updateSection_whenCapacityEqualsRegisteredCount_isAllowed() {
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(registrationRepository.countBySectionId(100L)).thenReturn(25L);

        Section updated = sectionService.updateSection(100L, new SectionRequest(null, null, 25));

        assertThat(updated.getCapacity()).isEqualTo(25);
    }

    @Test
    void updateSection_whenCapacityBelowRegisteredCount_throwsBR05() {
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(registrationRepository.countBySectionId(100L)).thenReturn(26L);

        assertThatThrownBy(() -> sectionService.updateSection(100L, new SectionRequest(null, null, 25)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-05");

        assertThat(section.getCapacity()).isEqualTo(40);
    }

    @Test
    void updateSection_whenSectionCancelled_throwsBusinessRuleException() {
        section.setStatus(SectionStatus.CANCELLED);
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));

        assertThatThrownBy(() -> sectionService.updateSection(100L, new SectionRequest(null, null, 25)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateSection_whenMissing_throwsResourceNotFoundException() {
        when(sectionRepository.findWithCourseById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.updateSection(999L, new SectionRequest(null, null, 25)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTeacherOptions_returnsOptionsWithQualificationFlag() {
        User user1 = User.builder().id(1L).universityId("T001").build();
        User user2 = User.builder().id(2L).universityId("T002").build();
        Teacher t1 = Teacher.builder().id(10L).fullName("Teacher A").user(user1).build();
        Teacher t2 = Teacher.builder().id(20L).fullName("Teacher B").user(user2).build();

        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findAll()).thenReturn(List.of(t1, t2));
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 5L)).thenReturn(false);

        List<TeacherAssignmentOptionResponse> options = sectionService.getTeacherOptions(100L);

        assertThat(options).hasSize(2);
        assertThat(options.get(0).teacherId()).isEqualTo(10L);
        assertThat(options.get(0).qualified()).isTrue();
        assertThat(options.get(1).teacherId()).isEqualTo(20L);
        assertThat(options.get(1).qualified()).isFalse();
    }

    @Test
    void assignTeacher_success_updatesSchedulesAndNotifies() {
        User userNew = User.builder().id(1L).universityId("T001").build();
        User userPrev = User.builder().id(2L).universityId("T002").build();
        User studentUser = User.builder().id(3L).universityId("S001").build();

        Teacher newTeacher = Teacher.builder().id(10L).fullName("Ajarn Somchai").user(userNew).build();
        Teacher prevTeacher = Teacher.builder().id(20L).fullName("Ajarn Somsri").user(userPrev).build();
        Student student = Student.builder().id(30L).fullName("Student A").user(studentUser).build();

        TimeSlot slot = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        Schedule schedule = Schedule.builder().id(50L).section(section).teacher(prevTeacher).timeSlot(slot).status(ScheduleStatus.PUBLISHED).build();
        Registration reg = Registration.builder().id(70L).section(section).student(student).build();

        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(newTeacher));
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(scheduleRepository.findBySectionId(100L)).thenReturn(List.of(schedule));
        when(teacherAvailabilityRepository.findByTeacherId(10L)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(10L, ScheduleStatus.PUBLISHED)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(10L, ScheduleStatus.DRAFT)).thenReturn(List.of());
        when(registrationRepository.findBySectionId(100L)).thenReturn(List.of(reg));

        Section result = sectionService.assignTeacher(100L, 10L);

        assertThat(result).isSameAs(section);
        assertThat(schedule.getTeacher()).isSameAs(newTeacher);
        verify(scheduleRepository).save(schedule);
        verify(notificationService).sendNotification(eq(userNew), eq(NotificationType.SCHEDULE_CHANGED), any(), any(), eq(NotificationChannel.IN_APP));
        verify(notificationService).sendNotification(eq(userPrev), eq(NotificationType.SCHEDULE_CHANGED), any(), any(), eq(NotificationChannel.IN_APP));
        verify(notificationService).sendNotification(eq(studentUser), eq(NotificationType.SCHEDULE_CHANGED), any(), any(), eq(NotificationChannel.IN_APP));
    }

    @Test
    void assignTeacher_whenSectionMissing_throwsResourceNotFound() {
        when(sectionRepository.findWithCourseById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.assignTeacher(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignTeacher_whenSectionCancelled_throwsBusinessRuleException() {
        section.setStatus(SectionStatus.CANCELLED);
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));

        assertThatThrownBy(() -> sectionService.assignTeacher(100L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ยกเลิกแล้ว");
    }

    @Test
    void assignTeacher_whenTeacherMissing_throwsResourceNotFound() {
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.assignTeacher(100L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignTeacher_whenNotQualified_throwsBR06() {
        Teacher teacher = Teacher.builder().id(10L).build();
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> sectionService.assignTeacher(100L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-06");
    }

    @Test
    void assignTeacher_whenNoSchedules_throwsBusinessRuleException() {
        Teacher teacher = Teacher.builder().id(10L).build();
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(scheduleRepository.findBySectionId(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> sectionService.assignTeacher(100L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ยังไม่มีคาบเรียน");
    }

    @Test
    void assignTeacher_whenSchedulesNotPublished_throwsBusinessRuleException() {
        Teacher teacher = Teacher.builder().id(10L).build();
        Schedule draftSchedule = Schedule.builder().id(50L).status(ScheduleStatus.DRAFT).build();
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(scheduleRepository.findBySectionId(100L)).thenReturn(List.of(draftSchedule));

        assertThatThrownBy(() -> sectionService.assignTeacher(100L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Publish");
    }

    @Test
    void assignTeacher_whenTeacherUnavailable_throwsBR07() {
        Teacher teacher = Teacher.builder().id(10L).build();
        TimeSlot slot = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        Schedule schedule = Schedule.builder().id(50L).timeSlot(slot).status(ScheduleStatus.PUBLISHED).build();
        TeacherAvailability unavail = TeacherAvailability.builder().id(1L).teacher(teacher).timeSlot(slot).isAvailable(false).build();

        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(scheduleRepository.findBySectionId(100L)).thenReturn(List.of(schedule));
        when(teacherAvailabilityRepository.findByTeacherId(10L)).thenReturn(List.of(unavail));

        assertThatThrownBy(() -> sectionService.assignTeacher(100L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-07");
    }

    @Test
    void assignTeacher_whenTeacherHasConflict_throwsBR01() {
        Teacher teacher = Teacher.builder().id(10L).build();
        TimeSlot slot = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        Schedule schedule = Schedule.builder().id(50L).section(section).timeSlot(slot).status(ScheduleStatus.PUBLISHED).build();

        Section otherSection = Section.builder().id(200L).build();
        Schedule conflictSchedule = Schedule.builder().id(99L).section(otherSection).timeSlot(slot).status(ScheduleStatus.PUBLISHED).build();

        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(scheduleRepository.findBySectionId(100L)).thenReturn(List.of(schedule));
        when(teacherAvailabilityRepository.findByTeacherId(10L)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(10L, ScheduleStatus.PUBLISHED)).thenReturn(List.of(conflictSchedule));

        assertThatThrownBy(() -> sectionService.assignTeacher(100L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-01");
    }

    @Test
    void assignTeacher_whenTeacherHasConflictInDraftSchedule_throwsBR01() {
        Teacher teacher = Teacher.builder().id(10L).build();
        TimeSlot slot = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        Schedule schedule = Schedule.builder().id(50L).section(section).timeSlot(slot).status(ScheduleStatus.PUBLISHED).build();

        Section otherSection = Section.builder().id(200L).build();
        Schedule draftConflict = Schedule.builder().id(99L).section(otherSection).timeSlot(slot).status(ScheduleStatus.DRAFT).build();

        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(scheduleRepository.findBySectionId(100L)).thenReturn(List.of(schedule));
        when(teacherAvailabilityRepository.findByTeacherId(10L)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(10L, ScheduleStatus.PUBLISHED)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(10L, ScheduleStatus.DRAFT)).thenReturn(List.of(draftConflict));

        assertThatThrownBy(() -> sectionService.assignTeacher(100L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT Timetable Conflict");
    }
}
