package com.project.acados.service;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.exception.BusinessRuleException;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.repository.AcademicEventRepository;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.SectionRepository;
import com.project.acados.repository.StudentRepository;
import com.project.acados.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final String UNIVERSITY_ID = "S001";
    private static final Long STUDENT_ID = 10L;
    private static final Long SECTION_ID = 100L;
    private static final Long OTHER_SECTION_ID = 200L;

    @Mock
    private AcademicEventRepository academicEventRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private User user;
    private Student student;
    private Course course;
    private Section section;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).universityId(UNIVERSITY_ID).email("s001@acados.local")
                .passwordHash("hashed").role(UserRole.STUDENT).build();
        student = Student.builder().id(STUDENT_ID).user(user).fullName("Student One").build();
        course = Course.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(3).build();
        section = Section.builder().id(SECTION_ID).course(course).sectionNumber(1).capacity(2).build();

        lenient().when(studentRepository.findByUserUniversityId(UNIVERSITY_ID)).thenReturn(Optional.of(student));
        lenient().when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        lenient().when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void insideRegistrationPeriod() {
        AcademicEvent period = AcademicEvent.builder()
                .eventName("Registration")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
        when(academicEventRepository.findActiveEventsByTypeAndDate(
                eq(AcademicEventType.REGISTRATION_PERIOD), any(LocalDate.class))).thenReturn(List.of(period));
    }

    private void outsideRegistrationPeriod() {
        when(academicEventRepository.findActiveEventsByTypeAndDate(
                eq(AcademicEventType.REGISTRATION_PERIOD), any(LocalDate.class))).thenReturn(List.of());
    }

    private Schedule period(Section ofSection, DayOfWeek day, int startHour, int endHour, ScheduleStatus status) {
        TimeSlot timeSlot = TimeSlot.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(endHour, 0))
                .build();
        return Schedule.builder().section(ofSection).timeSlot(timeSlot).status(status).build();
    }

    /** The student already holds a registration in another section with one period. */
    private void alreadyRegisteredIn(Schedule otherPeriod) {
        Section otherSection = otherPeriod.getSection();
        Registration existing = Registration.builder().id(900L).student(student).section(otherSection)
                .course(otherSection.getCourse()).build();
        when(registrationRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(existing));
        when(scheduleRepository.findBySectionId(OTHER_SECTION_ID)).thenReturn(List.of(otherPeriod));
    }

    private Section otherSection() {
        Course otherCourse = Course.builder().id(6L).courseCode("CP353001").title("Database").weeklyHours(3).build();
        return Section.builder().id(OTHER_SECTION_ID).course(otherCourse).sectionNumber(1).capacity(40).build();
    }

    @Test
    void testRegisterSuccess() {
        insideRegistrationPeriod();
        when(registrationRepository.existsByStudentIdAndCourseId(STUDENT_ID, 5L)).thenReturn(false);
        when(registrationRepository.countBySectionId(SECTION_ID)).thenReturn(1L);
        when(scheduleRepository.findBySectionId(SECTION_ID))
                .thenReturn(List.of(period(section, DayOfWeek.MONDAY, 9, 12, ScheduleStatus.PUBLISHED)));
        alreadyRegisteredIn(period(otherSection(), DayOfWeek.MONDAY, 13, 16, ScheduleStatus.PUBLISHED));

        Registration result = registrationService.register(UNIVERSITY_ID, SECTION_ID);

        assertThat(result.getStudent()).isSameAs(student);
        assertThat(result.getSection()).isSameAs(section);
        assertThat(result.getCourse()).isSameAs(course);
        verify(registrationRepository).save(any(Registration.class));
        verify(notificationService).sendNotification(eq(user), eq(NotificationType.REGISTRATION_SUCCESS),
                anyString(), anyString(), eq(NotificationChannel.IN_APP), eq(NotificationChannel.EMAIL));
    }

    @Test
    void testRejectOutsidePeriod() {
        outsideRegistrationPeriod();

        assertThatThrownBy(() -> registrationService.register(UNIVERSITY_ID, SECTION_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-09");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void testRejectCancelledSection() {
        insideRegistrationPeriod();
        section.setStatus(SectionStatus.CANCELLED);

        assertThatThrownBy(() -> registrationService.register(UNIVERSITY_ID, SECTION_ID))
                .isInstanceOf(BusinessRuleException.class);

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void testRejectDuplicateCourse() {
        insideRegistrationPeriod();
        when(registrationRepository.existsByStudentIdAndCourseId(STUDENT_ID, 5L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(UNIVERSITY_ID, SECTION_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-04");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void testRejectSectionFull() {
        insideRegistrationPeriod();
        when(registrationRepository.existsByStudentIdAndCourseId(STUDENT_ID, 5L)).thenReturn(false);
        when(registrationRepository.countBySectionId(SECTION_ID)).thenReturn(2L);

        assertThatThrownBy(() -> registrationService.register(UNIVERSITY_ID, SECTION_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-05");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void testRejectScheduleConflict() {
        insideRegistrationPeriod();
        when(registrationRepository.existsByStudentIdAndCourseId(STUDENT_ID, 5L)).thenReturn(false);
        when(registrationRepository.countBySectionId(SECTION_ID)).thenReturn(0L);
        when(scheduleRepository.findBySectionId(SECTION_ID))
                .thenReturn(List.of(period(section, DayOfWeek.MONDAY, 9, 12, ScheduleStatus.PUBLISHED)));
        alreadyRegisteredIn(period(otherSection(), DayOfWeek.MONDAY, 10, 13, ScheduleStatus.PUBLISHED));

        assertThatThrownBy(() -> registrationService.register(UNIVERSITY_ID, SECTION_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-03");

        verify(notificationService).sendNotificationInNewTransaction(eq(user),
                eq(NotificationType.CONFLICT_DETECTED), anyString(), anyString(), eq(NotificationChannel.IN_APP));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void testNoConflictWhenOverlappingPeriodIsDraft() {
        insideRegistrationPeriod();
        when(registrationRepository.existsByStudentIdAndCourseId(STUDENT_ID, 5L)).thenReturn(false);
        when(registrationRepository.countBySectionId(SECTION_ID)).thenReturn(0L);
        when(scheduleRepository.findBySectionId(SECTION_ID))
                .thenReturn(List.of(period(section, DayOfWeek.MONDAY, 9, 12, ScheduleStatus.PUBLISHED)));
        alreadyRegisteredIn(period(otherSection(), DayOfWeek.MONDAY, 10, 13, ScheduleStatus.DRAFT));

        Registration result = registrationService.register(UNIVERSITY_ID, SECTION_ID);

        assertThat(result.getSection()).isSameAs(section);
    }

    @Test
    void testNoConflictWhenPeriodsAreBackToBack() {
        insideRegistrationPeriod();
        when(registrationRepository.existsByStudentIdAndCourseId(STUDENT_ID, 5L)).thenReturn(false);
        when(registrationRepository.countBySectionId(SECTION_ID)).thenReturn(0L);
        when(scheduleRepository.findBySectionId(SECTION_ID))
                .thenReturn(List.of(period(section, DayOfWeek.MONDAY, 9, 12, ScheduleStatus.PUBLISHED)));
        alreadyRegisteredIn(period(otherSection(), DayOfWeek.MONDAY, 12, 15, ScheduleStatus.PUBLISHED));

        Registration result = registrationService.register(UNIVERSITY_ID, SECTION_ID);

        assertThat(result.getSection()).isSameAs(section);
    }

    @Test
    void testRejectUnknownStudent() {
        when(studentRepository.findByUserUniversityId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register("UNKNOWN", SECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testRejectUnknownSection() {
        insideRegistrationPeriod();
        when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(UNIVERSITY_ID, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testWithdrawSuccess() {
        insideRegistrationPeriod();
        Registration registration = Registration.builder().id(900L).student(student).section(section)
                .course(course).build();
        when(registrationRepository.findByIdAndStudentId(900L, STUDENT_ID)).thenReturn(Optional.of(registration));

        registrationService.withdraw(UNIVERSITY_ID, 900L);

        verify(registrationRepository).delete(registration);
        verify(notificationService).sendNotification(eq(user), eq(NotificationType.REGISTRATION_WITHDRAWN),
                anyString(), anyString(), eq(NotificationChannel.IN_APP));
    }

    @Test
    void testWithdrawRejectOutsidePeriod() {
        outsideRegistrationPeriod();

        assertThatThrownBy(() -> registrationService.withdraw(UNIVERSITY_ID, 900L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-09");

        verify(registrationRepository, never()).delete(any());
    }

    @Test
    void testWithdrawRejectRegistrationOfAnotherStudent() {
        insideRegistrationPeriod();
        when(registrationRepository.findByIdAndStudentId(901L, STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.withdraw(UNIVERSITY_ID, 901L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(registrationRepository, never()).delete(any());
    }
}
