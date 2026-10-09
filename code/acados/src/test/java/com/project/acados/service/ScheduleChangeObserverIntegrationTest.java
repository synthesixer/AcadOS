package com.project.acados.service;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.notification.strategy.InAppNotificationStrategy;
import com.project.acados.pattern.observer.ScheduleChangePublisher;
import com.project.acados.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Observer Pattern end to end: the real ScheduleChangePublisher notifies the real
 * NotificationServiceImpl, which stores SCHEDULE_CHANGED notifications in the test database.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({NotificationServiceImpl.class, InAppNotificationStrategy.class, ScheduleChangePublisher.class})
class ScheduleChangeObserverIntegrationTest {

    @Autowired
    private ScheduleChangePublisher scheduleChangePublisher;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TestEntityManager entityManager;

    private User persistUser(String universityId, UserRole role) {
        return entityManager.persist(User.builder()
                .universityId(universityId)
                .email(universityId.toLowerCase() + "@acados.local")
                .passwordHash("hashed")
                .role(role)
                .build());
    }

    @Test
    void notifyObservers_sendsScheduleChangedToStudentAndTeacherOfTheSection() {
        User studentUser = persistUser("S001", UserRole.STUDENT);
        User otherStudentUser = persistUser("S002", UserRole.STUDENT);
        User teacherUser = persistUser("T001", UserRole.TEACHER);
        Student student = entityManager.persist(Student.builder().user(studentUser).fullName("Student One").build());
        entityManager.persist(Student.builder().user(otherStudentUser).fullName("Student Two").build());
        Teacher teacher = entityManager.persist(Teacher.builder().user(teacherUser).fullName("Teacher One").build());

        Course course = entityManager.persist(Course.builder()
                .courseCode("CP353002").title("Software Design").weeklyHours(3).build());
        Section section = entityManager.persist(Section.builder()
                .course(course).sectionNumber(1).capacity(40).build());
        TimeSlot timeSlot = entityManager.persist(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build());
        entityManager.persist(Schedule.builder()
                .section(section).teacher(teacher).timeSlot(timeSlot).status(ScheduleStatus.PUBLISHED).build());
        entityManager.persist(Registration.builder().student(student).section(section).course(course).build());
        entityManager.flush();

        scheduleChangePublisher.notifyObservers(List.of(section));
        entityManager.flush();
        entityManager.clear();

        List<Notification> studentNotifications = notificationService.getNotifications("S001");
        assertThat(studentNotifications).hasSize(1);
        assertThat(studentNotifications.get(0).getType()).isEqualTo(NotificationType.SCHEDULE_CHANGED);
        assertThat(studentNotifications.get(0).getMessage()).contains("CP353002 Section 1");
        assertThat(notificationService.getNotifications("T001")).hasSize(1);
        assertThat(notificationService.getNotifications("S002")).isEmpty();
    }
}
