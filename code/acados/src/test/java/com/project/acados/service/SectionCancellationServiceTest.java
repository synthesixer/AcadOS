package com.project.acados.service;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.domain.enums.SwapStatus;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.exception.BusinessRuleException;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.SectionRepository;
import com.project.acados.repository.TeacherSwapRequestRepository;
import com.project.acados.service.impl.SectionCancellationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionCancellationServiceTest {

    private static final Long SECTION_ID = 100L;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TeacherSwapRequestRepository swapRequestRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SectionCancellationServiceImpl cancellationService;

    private Section section;
    private User studentUser;
    private User teacherUser;
    private User otherTeacherUser;
    private Teacher teacher;
    private Teacher otherTeacher;
    private Schedule period;

    @BeforeEach
    void setUp() {
        Course course = Course.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(3).build();
        section = Section.builder().id(SECTION_ID).course(course).sectionNumber(1).capacity(40).build();
        studentUser = user(1L, "S001", UserRole.STUDENT);
        teacherUser = user(2L, "T001", UserRole.TEACHER);
        otherTeacherUser = user(3L, "T002", UserRole.TEACHER);
        teacher = Teacher.builder().id(20L).user(teacherUser).fullName("Teacher One").build();
        otherTeacher = Teacher.builder().id(30L).user(otherTeacherUser).fullName("Teacher Two").build();
        period = Schedule.builder().id(500L).section(section).teacher(teacher).status(ScheduleStatus.PUBLISHED).build();
    }

    private User user(Long id, String universityId, UserRole role) {
        return User.builder().id(id).universityId(universityId).email(universityId.toLowerCase() + "@acados.local")
                .passwordHash("hashed").role(role).build();
    }

    private void sectionHasOneStudentAndOnePeriod() {
        Student student = Student.builder().id(10L).user(studentUser).fullName("Student One").build();
        when(sectionRepository.findWithCourseById(SECTION_ID)).thenReturn(Optional.of(section));
        when(scheduleRepository.findBySectionId(SECTION_ID)).thenReturn(List.of(period));
        when(registrationRepository.findBySectionId(SECTION_ID)).thenReturn(List.of(
                Registration.builder().id(900L).student(student).section(section).course(section.getCourse()).build()));
    }

    private TeacherSwapRequest swapRequest(Schedule requestingPeriod, Schedule targetPeriod, SwapStatus status) {
        return TeacherSwapRequest.builder()
                .id(700L)
                .requestingTeacher(teacher)
                .requestingSchedule(requestingPeriod)
                .targetTeacher(otherTeacher)
                .targetSchedule(targetPeriod)
                .status(status)
                .build();
    }

    private Schedule periodOfAnotherSection() {
        Section otherSection = Section.builder().id(200L).sectionNumber(1).capacity(40).build();
        return Schedule.builder().id(600L).section(otherSection).teacher(otherTeacher)
                .status(ScheduleStatus.PUBLISHED).build();
    }

    @Test
    void cancelSection_cancelsDeletesRegistrationsReleasesPeriodsAndNotifies() {
        sectionHasOneStudentAndOnePeriod();
        when(swapRequestRepository.findAll()).thenReturn(List.of());

        cancellationService.cancelSection(SECTION_ID);

        assertThat(section.getStatus()).isEqualTo(SectionStatus.CANCELLED);
        verify(registrationRepository).deleteBySectionId(SECTION_ID);
        verify(scheduleRepository).deleteAll(List.of(period));
        verify(notificationService).sendNotification(eq(studentUser), eq(NotificationType.SECTION_CANCELLED),
                anyString(), anyString(), eq(NotificationChannel.IN_APP));
        verify(notificationService).sendNotification(eq(teacherUser), eq(NotificationType.SECTION_CANCELLED),
                anyString(), anyString(), eq(NotificationChannel.IN_APP));
        verify(notificationService, times(2)).sendNotification(any(), any(), anyString(), anyString(),
                any(NotificationChannel[].class));
    }

    @Test
    void cancelSection_whenOpenSwapRefersToPeriod_detachesCancelsAndNotifiesBothTeachers() {
        sectionHasOneStudentAndOnePeriod();
        TeacherSwapRequest pending = swapRequest(period, periodOfAnotherSection(), SwapStatus.PENDING);
        when(swapRequestRepository.findAll()).thenReturn(List.of(pending));

        cancellationService.cancelSection(SECTION_ID);

        assertThat(pending.getRequestingSchedule()).isNull();
        assertThat(pending.getTargetSchedule()).isNotNull();
        assertThat(pending.getStatus()).isEqualTo(SwapStatus.CANCELLED);
        verify(notificationService).sendNotification(eq(otherTeacherUser), eq(NotificationType.SECTION_CANCELLED),
                anyString(), anyString(), eq(NotificationChannel.IN_APP));
        verify(notificationService, times(3)).sendNotification(any(), any(), anyString(), anyString(),
                any(NotificationChannel[].class));
    }

    @Test
    void cancelSection_whenFinishedSwapRefersToPeriod_detachesButKeepsItsStatus() {
        sectionHasOneStudentAndOnePeriod();
        TeacherSwapRequest approved = swapRequest(periodOfAnotherSection(), period, SwapStatus.APPROVED);
        when(swapRequestRepository.findAll()).thenReturn(List.of(approved));

        cancellationService.cancelSection(SECTION_ID);

        assertThat(approved.getTargetSchedule()).isNull();
        assertThat(approved.getStatus()).isEqualTo(SwapStatus.APPROVED);
    }

    @Test
    void cancelSection_whenSwapRefersToOtherSections_leavesItUntouched() {
        sectionHasOneStudentAndOnePeriod();
        Schedule unrelatedPeriod = periodOfAnotherSection();
        TeacherSwapRequest unrelated = swapRequest(unrelatedPeriod, unrelatedPeriod, SwapStatus.PENDING);
        when(swapRequestRepository.findAll()).thenReturn(List.of(unrelated));

        cancellationService.cancelSection(SECTION_ID);

        assertThat(unrelated.getRequestingSchedule()).isSameAs(unrelatedPeriod);
        assertThat(unrelated.getStatus()).isEqualTo(SwapStatus.PENDING);
        verify(notificationService, never()).sendNotification(eq(otherTeacherUser), any(), anyString(),
                anyString(), any(NotificationChannel[].class));
    }

    @Test
    void cancelSection_whenAlreadyCancelled_throwsAndChangesNothing() {
        section.setStatus(SectionStatus.CANCELLED);
        when(sectionRepository.findWithCourseById(SECTION_ID)).thenReturn(Optional.of(section));

        assertThatThrownBy(() -> cancellationService.cancelSection(SECTION_ID))
                .isInstanceOf(BusinessRuleException.class);

        verify(registrationRepository, never()).deleteBySectionId(any());
        verify(scheduleRepository, never()).deleteAll(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancelSection_whenSectionMissing_throwsResourceNotFoundException() {
        when(sectionRepository.findWithCourseById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cancellationService.cancelSection(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
