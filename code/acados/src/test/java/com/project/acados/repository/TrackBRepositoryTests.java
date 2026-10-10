package com.project.acados.repository;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.domain.enums.SwapStatus;
import com.project.acados.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TrackBRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherQualificationRepository qualificationRepository;

    @Autowired
    private TeacherPreferenceRepository preferenceRepository;

    @Autowired
    private TeacherAvailabilityRepository availabilityRepository;

    @Autowired
    private TeacherSwapRequestRepository swapRequestRepository;

    private User createSampleUser(String universityId, String email, UserRole role) {
        return userRepository.save(User.builder()
                .universityId(universityId)
                .email(email)
                .passwordHash("hashed-password")
                .role(role)
                .build());
    }

    private Teacher createSampleTeacher(String universityId, String email, String fullName) {
        User user = createSampleUser(universityId, email, UserRole.TEACHER);
        return teacherRepository.save(Teacher.builder()
                .user(user)
                .fullName(fullName)
                .build());
    }

    private Course createSampleCourse(String code, String title) {
        Course course = Course.builder()
                .courseCode(code)
                .title(title)
                .weeklyHours(3)
                .build();
        return entityManager.persist(course);
    }

    @Test
    @DisplayName("UserRepository: should save and find by universityId and email")
    void testUserRepository() {
        createSampleUser("ADMIN01", "admin@kku.ac.th", UserRole.ADMIN);

        Optional<User> byUnivId = userRepository.findByUniversityId("ADMIN01");
        assertThat(byUnivId).isPresent();
        assertThat(byUnivId.get().getRole()).isEqualTo(UserRole.ADMIN);

        assertThat(userRepository.existsByEmail("admin@kku.ac.th")).isTrue();
        assertThat(userRepository.existsByUniversityId("NONEXISTENT")).isFalse();
    }

    @Test
    @DisplayName("TeacherRepository: should find teacher by userId")
    void testTeacherRepository() {
        Teacher teacher = createSampleTeacher("T001", "teacher1@kku.ac.th", "Dr. Somchai");

        Optional<Teacher> found = teacherRepository.findByUserId(teacher.getUser().getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Dr. Somchai");
        assertThat(teacherRepository.existsByUserId(teacher.getUser().getId())).isTrue();
    }

    @Test
    @DisplayName("TeacherQualificationRepository: should check qualification existence")
    void testTeacherQualificationRepository() {
        Teacher teacher = createSampleTeacher("T002", "teacher2@kku.ac.th", "Dr. Somsak");
        Course course = createSampleCourse("CP351001", "Data Structures");

        TeacherQualification qual = TeacherQualification.builder()
                .teacher(teacher)
                .course(course)
                .build();
        qualificationRepository.save(qual);

        boolean exists = qualificationRepository.existsByTeacherIdAndCourseId(teacher.getId(), course.getId());
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("TeacherPreferenceRepository: should find preferences ordered by priority")
    void testTeacherPreferenceRepository() {
        Teacher teacher = createSampleTeacher("T003", "teacher3@kku.ac.th", "Dr. Wichai");
        Course c1 = createSampleCourse("CP352001", "Algorithms");
        Course c2 = createSampleCourse("CP352002", "Software Architecture");

        preferenceRepository.save(TeacherPreference.builder().teacher(teacher).course(c1).priority(2).build());
        preferenceRepository.save(TeacherPreference.builder().teacher(teacher).course(c2).priority(1).build());

        List<TeacherPreference> ordered = preferenceRepository.findByTeacherIdOrderByPriorityAsc(teacher.getId());
        assertThat(ordered).hasSize(2);
        assertThat(ordered.get(0).getCourse().getCourseCode()).isEqualTo("CP352002");
        assertThat(ordered.get(1).getCourse().getCourseCode()).isEqualTo("CP352001");
    }

    @Test
    @DisplayName("TeacherAvailabilityRepository: should check availability for teacher and timeslot")
    void testTeacherAvailabilityRepository() {
        Teacher teacher = createSampleTeacher("T004", "teacher4@kku.ac.th", "Dr. Prasert");
        TimeSlot slot = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.FRIDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();
        entityManager.persist(slot);

        availabilityRepository.save(TeacherAvailability.builder()
                .teacher(teacher)
                .timeSlot(slot)
                .isAvailable(false)
                .build());

        Optional<TeacherAvailability> found = availabilityRepository.findByTeacherIdAndTimeSlotId(teacher.getId(), slot.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isAvailable()).isFalse();
        assertThat(availabilityRepository.existsByTeacherIdAndTimeSlotId(teacher.getId(), slot.getId())).isTrue();
    }

    @Test
    @DisplayName("TeacherSwapRequestRepository: should save swap and prevent double open swap")
    void testTeacherSwapRequestRepository() {
        Teacher teacherA = createSampleTeacher("T005", "teacher5@kku.ac.th", "Teacher A");
        Teacher teacherB = createSampleTeacher("T006", "teacher6@kku.ac.th", "Teacher B");

        Course course = createSampleCourse("CP353002", "Software Architecture");
        Section section = Section.builder().course(course).sectionNumber(1).capacity(30).status(SectionStatus.ACTIVE).build();
        entityManager.persist(section);

        TimeSlot slotA = TimeSlot.builder().dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        TimeSlot slotB = TimeSlot.builder().dayOfWeek(DayOfWeek.TUESDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        entityManager.persist(slotA);
        entityManager.persist(slotB);

        Schedule scheduleA = Schedule.builder().section(section).teacher(teacherA).timeSlot(slotA).status(ScheduleStatus.PUBLISHED).build();
        Schedule scheduleB = Schedule.builder().section(section).teacher(teacherB).timeSlot(slotB).status(ScheduleStatus.PUBLISHED).build();
        entityManager.persist(scheduleA);
        entityManager.persist(scheduleB);

        TeacherSwapRequest request = TeacherSwapRequest.builder()
                .requestingTeacher(teacherA)
                .requestingSchedule(scheduleA)
                .targetTeacher(teacherB)
                .targetSchedule(scheduleB)
                .status(SwapStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        swapRequestRepository.save(request);

        assertThat(request.getId()).isNotNull();

        // Test Double Open Swap prevention query
        boolean hasOpenSwap = swapRequestRepository.existsOpenSwapForSchedule(
                scheduleA.getId(), List.of(SwapStatus.PENDING, SwapStatus.ACCEPTED)
        );
        assertThat(hasOpenSwap).isTrue();

        // Transition domain method
        request.accept();
        swapRequestRepository.save(request);
        assertThat(request.getStatus()).isEqualTo(SwapStatus.ACCEPTED);
        assertThat(request.getRespondedAt()).isNotNull();

        request.approve();
        swapRequestRepository.save(request);
        assertThat(request.getStatus()).isEqualTo(SwapStatus.APPROVED);
        assertThat(request.getReviewedAt()).isNotNull();
    }
}
