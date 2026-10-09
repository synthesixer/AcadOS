package com.project.acados.repository;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class TrackCRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User persistUser(String universityId) {
        User user = User.builder()
                .universityId(universityId)
                .email(universityId.toLowerCase() + "@acados.local")
                .passwordHash("hashed")
                .role(UserRole.STUDENT)
                .build();
        return entityManager.persist(user);
    }

    private Student persistStudent(String universityId) {
        Student student = Student.builder()
                .user(persistUser(universityId))
                .fullName("Student " + universityId)
                .build();
        return entityManager.persist(student);
    }

    private Course persistCourse(String courseCode) {
        Course course = Course.builder()
                .courseCode(courseCode)
                .title("Course " + courseCode)
                .weeklyHours(3)
                .build();
        return entityManager.persist(course);
    }

    private Section persistSection(Course course, int sectionNumber) {
        Section section = Section.builder()
                .course(course)
                .sectionNumber(sectionNumber)
                .capacity(40)
                .build();
        return entityManager.persist(section);
    }

    private Registration register(Student student, Section section) {
        return Registration.builder()
                .student(student)
                .section(section)
                .course(section.getCourse())
                .build();
    }

    @Test
    @DisplayName("StudentRepository: should find student profile by user id")
    void testStudentRepository() {
        Student student = persistStudent("S001");

        assertThat(studentRepository.findByUserId(student.getUser().getId())).isPresent();
        assertThat(studentRepository.existsByUserId(student.getUser().getId())).isTrue();
        assertThat(studentRepository.findByUserId(-1L)).isEmpty();
    }

    @Test
    @DisplayName("SectionRepository: should default to ACTIVE and query by course and status")
    void testSectionRepository() {
        Course course = persistCourse("CP353002");
        Section section = persistSection(course, 1);
        persistSection(course, 2);

        assertThat(section.getStatus()).isEqualTo(SectionStatus.ACTIVE);
        assertThat(sectionRepository.findByCourseId(course.getId())).hasSize(2);
        assertThat(sectionRepository.findByStatus(SectionStatus.ACTIVE)).hasSize(2);
        assertThat(sectionRepository.findByStatus(SectionStatus.CANCELLED)).isEmpty();
        assertThat(sectionRepository.existsByCourseId(course.getId())).isTrue();
        assertThat(sectionRepository.existsByCourseIdAndSectionNumber(course.getId(), 1)).isTrue();
        assertThat(sectionRepository.existsByCourseIdAndSectionNumber(course.getId(), 3)).isFalse();
    }

    @Test
    @DisplayName("RegistrationRepository: should set registeredAt and support registration rule queries")
    void testRegistrationRepository() {
        Student student = persistStudent("S001");
        Student other = persistStudent("S002");
        Section section = persistSection(persistCourse("CP353002"), 1);

        Registration saved = registrationRepository.saveAndFlush(register(student, section));
        registrationRepository.saveAndFlush(register(other, section));

        assertThat(saved.getRegisteredAt()).isNotNull();
        assertThat(registrationRepository.countBySectionId(section.getId())).isEqualTo(2);
        assertThat(registrationRepository.existsByStudentIdAndCourseId(
                student.getId(), section.getCourse().getId())).isTrue();
        assertThat(registrationRepository.existsByStudentId(student.getId())).isTrue();
        assertThat(registrationRepository.findByStudentId(student.getId())).hasSize(1);
        assertThat(registrationRepository.findBySectionId(section.getId())).hasSize(2);
        assertThat(registrationRepository.findByIdAndStudentId(saved.getId(), student.getId())).isPresent();
        assertThat(registrationRepository.findByIdAndStudentId(saved.getId(), other.getId())).isEmpty();
    }

    @Test
    @DisplayName("RegistrationRepository: should reject a second section of the same course (BR-04)")
    void testRegistrationUniqueStudentCourse() {
        Student student = persistStudent("S001");
        Course course = persistCourse("CP353002");
        Section section1 = persistSection(course, 1);
        Section section2 = persistSection(course, 2);

        registrationRepository.saveAndFlush(register(student, section1));

        assertThatThrownBy(() -> registrationRepository.saveAndFlush(register(student, section2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("RegistrationRepository: should delete all registrations of a section")
    void testRegistrationDeleteBySection() {
        Section section = persistSection(persistCourse("CP353002"), 1);
        Section untouched = persistSection(persistCourse("CP353001"), 1);
        Student student = persistStudent("S001");
        registrationRepository.saveAndFlush(register(student, section));
        registrationRepository.saveAndFlush(register(student, untouched));

        registrationRepository.deleteBySectionId(section.getId());
        entityManager.flush();

        assertThat(registrationRepository.countBySectionId(section.getId())).isZero();
        assertThat(registrationRepository.countBySectionId(untouched.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("NotificationRepository: should default to unread, order newest first and count unread")
    void testNotificationRepository() {
        User user = persistUser("S001");
        User otherUser = persistUser("S002");

        Notification older = notificationRepository.saveAndFlush(Notification.builder()
                .user(user)
                .title("Registration successful")
                .message("You are registered in CP353002 section 1.")
                .type(NotificationType.REGISTRATION_SUCCESS)
                .createdAt(LocalDateTime.of(2026, 10, 10, 9, 0))
                .build());
        Notification newer = notificationRepository.saveAndFlush(Notification.builder()
                .user(user)
                .title("Schedule changed")
                .message("The timetable of CP353002 section 1 has changed.")
                .type(NotificationType.SCHEDULE_CHANGED)
                .createdAt(LocalDateTime.of(2026, 10, 10, 10, 0))
                .build());

        assertThat(older.getIsRead()).isFalse();

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(notifications).extracting(Notification::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isEqualTo(2);

        older.markAsRead();
        notificationRepository.saveAndFlush(older);

        assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isEqualTo(1);
        assertThat(notificationRepository.findByIdAndUserId(older.getId(), user.getId())).isPresent();
        assertThat(notificationRepository.findByIdAndUserId(older.getId(), otherUser.getId())).isEmpty();
    }
}
