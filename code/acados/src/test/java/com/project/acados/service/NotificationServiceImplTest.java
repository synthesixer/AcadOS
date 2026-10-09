package com.project.acados.service;

import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.Registration;
import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Student;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.pattern.observer.ScheduleChangeSubject;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.notification.strategy.NotificationMessage;
import com.project.acados.notification.strategy.NotificationStrategy;
import com.project.acados.repository.NotificationRepository;
import com.project.acados.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.acados.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationStrategy inAppStrategy;

    @Mock
    private NotificationStrategy emailStrategy;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleChangeSubject scheduleChangeSubject;

    private NotificationServiceImpl notificationService;

    private User recipient;

    @BeforeEach
    void setUp() {
        lenient().when(inAppStrategy.supports(any()))
                .thenAnswer(invocation -> invocation.getArgument(0) == NotificationChannel.IN_APP);
        lenient().when(emailStrategy.supports(any()))
                .thenAnswer(invocation -> invocation.getArgument(0) == NotificationChannel.EMAIL);
        notificationService = new NotificationServiceImpl(List.of(inAppStrategy, emailStrategy),
                notificationRepository, registrationRepository, scheduleRepository, scheduleChangeSubject);
        recipient = User.builder()
                .id(1L)
                .universityId("S001")
                .email("s001@acados.local")
                .passwordHash("hashed")
                .role(UserRole.STUDENT)
                .build();
    }

    @Test
    void sendNotification_whenInAppOnly_usesOnlyInAppStrategy() {
        notificationService.sendNotification(recipient, NotificationType.CONFLICT_DETECTED,
                "Schedule conflict", "The section overlaps with a registered section.",
                NotificationChannel.IN_APP);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(inAppStrategy).send(captor.capture());
        verify(emailStrategy, never()).send(any());

        NotificationMessage sent = captor.getValue();
        assertThat(sent.recipient()).isSameAs(recipient);
        assertThat(sent.type()).isEqualTo(NotificationType.CONFLICT_DETECTED);
        assertThat(sent.title()).isEqualTo("Schedule conflict");
        assertThat(sent.message()).isEqualTo("The section overlaps with a registered section.");
    }

    @Test
    void sendNotification_whenInAppAndEmail_usesBothStrategies() {
        notificationService.sendNotification(recipient, NotificationType.REGISTRATION_SUCCESS,
                "Registration successful", "You are registered.",
                NotificationChannel.IN_APP, NotificationChannel.EMAIL);

        verify(inAppStrategy).send(any());
        verify(emailStrategy).send(any());
    }

    @Test
    void sendNotification_whenChannelRepeated_sendsOncePerChannel() {
        notificationService.sendNotification(recipient, NotificationType.SCHEDULE_CHANGED,
                "Schedule changed", "The timetable has changed.",
                NotificationChannel.IN_APP, NotificationChannel.IN_APP);

        verify(inAppStrategy, times(1)).send(any());
    }

    @Test
    void sendNotification_whenNoChannel_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> notificationService.sendNotification(recipient,
                NotificationType.SCHEDULE_CHANGED, "Schedule changed", "The timetable has changed."))
                .isInstanceOf(IllegalArgumentException.class);

        verify(inAppStrategy, never()).send(any());
        verify(emailStrategy, never()).send(any());
    }

    @Test
    void getNotifications_returnsUserNotificationsFromRepository() {
        Notification notification = Notification.builder()
                .id(10L)
                .user(recipient)
                .type(NotificationType.SCHEDULE_CHANGED)
                .title("Schedule changed")
                .message("The timetable has changed.")
                .build();
        when(notificationRepository.findByUserUniversityIdOrderByCreatedAtDesc("S001"))
                .thenReturn(List.of(notification));

        List<Notification> result = notificationService.getNotifications("S001");

        assertThat(result).containsExactly(notification);
    }

    @Test
    void sendNotificationInNewTransaction_usesRequestedStrategy() {
        notificationService.sendNotificationInNewTransaction(recipient, NotificationType.CONFLICT_DETECTED,
                "Schedule conflict", "The section overlaps with a registered section.",
                NotificationChannel.IN_APP);

        verify(inAppStrategy).send(any());
        verify(emailStrategy, never()).send(any());
    }

    @Test
    void markAsRead_whenOwnNotification_marksItRead() {
        Notification notification = Notification.builder()
                .id(10L)
                .user(recipient)
                .type(NotificationType.SCHEDULE_CHANGED)
                .title("Schedule changed")
                .message("The timetable has changed.")
                .build();
        when(notificationRepository.findByIdAndUserUniversityId(10L, "S001")).thenReturn(Optional.of(notification));

        notificationService.markAsRead("S001", 10L);

        assertThat(notification.getIsRead()).isTrue();
    }

    private User user(Long id, String universityId, UserRole role) {
        return User.builder().id(id).universityId(universityId).email(universityId.toLowerCase() + "@acados.local")
                .passwordHash("hashed").role(role).build();
    }

    private Schedule periodTaughtBy(User teacherUser, Section section, ScheduleStatus status) {
        Teacher teacher = Teacher.builder().id(teacherUser.getId()).user(teacherUser).fullName("Teacher").build();
        return Schedule.builder().section(section).teacher(teacher).status(status).build();
    }

    @Test
    void onScheduleChanged_notifiesRegisteredStudentsAndTeachersOfPublishedPeriodsOnce() {
        Course course = Course.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(6).build();
        Section section = Section.builder().id(100L).course(course).sectionNumber(1).capacity(40).build();
        User teacherUser = user(2L, "T001", UserRole.TEACHER);
        User draftTeacherUser = user(3L, "T002", UserRole.TEACHER);
        Student student = Student.builder().id(10L).user(recipient).fullName("Student One").build();
        when(registrationRepository.findBySectionId(100L)).thenReturn(List.of(
                Registration.builder().id(900L).student(student).section(section).course(course).build()));
        when(scheduleRepository.findBySectionId(100L)).thenReturn(List.of(
                periodTaughtBy(teacherUser, section, ScheduleStatus.PUBLISHED),
                periodTaughtBy(teacherUser, section, ScheduleStatus.PUBLISHED),
                periodTaughtBy(draftTeacherUser, section, ScheduleStatus.DRAFT)));

        notificationService.onScheduleChanged(List.of(section));

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(inAppStrategy, times(2)).send(captor.capture());
        verify(emailStrategy, never()).send(any());
        assertThat(captor.getAllValues()).extracting(NotificationMessage::recipient)
                .containsExactly(recipient, teacherUser);
        assertThat(captor.getAllValues()).extracting(NotificationMessage::type)
                .containsOnly(NotificationType.SCHEDULE_CHANGED);
        assertThat(captor.getAllValues().get(0).message()).contains("CP353002 Section 1");
    }

    @Test
    void onScheduleChanged_whenNoSections_sendsNothing() {
        notificationService.onScheduleChanged(List.of());
        notificationService.onScheduleChanged(null);

        verify(inAppStrategy, never()).send(any());
    }

    @Test
    void markAsRead_whenNotificationOfAnotherUser_throwsResourceNotFoundException() {
        when(notificationRepository.findByIdAndUserUniversityId(10L, "S002")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("S002", 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
