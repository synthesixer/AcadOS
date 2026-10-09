package com.project.acados.repository;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TrackARepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AcademicEventRepository academicEventRepository;

    @Autowired
    private PublicHolidayRepository publicHolidayRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("CourseRepository: should save and find course by course code")
    void testCourseRepository() {
        Course course = Course.builder()
                .courseCode("CP353002")
                .title("Software Quality Assurance")
                .weeklyHours(3)
                .build();
        courseRepository.save(course);

        Optional<Course> found = courseRepository.findByCourseCode("CP353002");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Software Quality Assurance");
        assertThat(courseRepository.existsByCourseCode("CP353002")).isTrue();
    }

    @Test
    @DisplayName("RoomRepository: should find room by building and room number and filter by capacity")
    void testRoomRepository() {
        Room room = Room.builder()
                .building("SC01")
                .roomNumber("101")
                .floor(1)
                .capacity(50)
                .isAvailable(true)
                .build();
        roomRepository.save(room);

        Optional<Room> found = roomRepository.findByBuildingAndRoomNumber("SC01", "101");
        assertThat(found).isPresent();
        assertThat(found.get().getCapacity()).isEqualTo(50);

        List<Room> capableRooms = roomRepository.findByCapacityGreaterThanEqualAndIsAvailableTrue(40);
        assertThat(capableRooms).hasSize(1);
    }

    @Test
    @DisplayName("TimeSlotRepository: should find time slot by day and time, and check overlap helper")
    void testTimeSlotRepository() {
        TimeSlot slot1 = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();
        timeSlotRepository.save(slot1);

        TimeSlot slot2 = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(13, 0))
                .build();

        TimeSlot slotTuesday = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        assertThat(slot1.overlapsWith(slot2)).isTrue();
        assertThat(slot1.overlapsWith(slotTuesday)).isFalse();

        Optional<TimeSlot> found = timeSlotRepository.findByDayOfWeekAndStartTimeAndEndTime(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)
        );
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("AcademicEventRepository: should query active event by date range")
    void testAcademicEventRepository() {
        AcademicEvent event = AcademicEvent.builder()
                .eventName("Add/Drop Period Semester 1")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 15))
                .build();
        academicEventRepository.save(event);

        List<AcademicEvent> activeEvents = academicEventRepository.findActiveEventsByTypeAndDate(
                AcademicEventType.REGISTRATION_PERIOD, LocalDate.of(2026, 8, 5)
        );
        assertThat(activeEvents).hasSize(1);
        assertThat(activeEvents.get(0).getEventName()).isEqualTo("Add/Drop Period Semester 1");
    }

    @Test
    @DisplayName("PublicHolidayRepository: should query holidays between dates")
    void testPublicHolidayRepository() {
        PublicHoliday holiday = PublicHoliday.builder()
                .date(LocalDate.of(2026, 1, 1))
                .name("New Year's Day")
                .description("Public holiday")
                .build();
        publicHolidayRepository.save(holiday);

        List<PublicHoliday> holidays = publicHolidayRepository.findByDateBetween(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)
        );
        assertThat(holidays).hasSize(1);
        assertThat(publicHolidayRepository.existsByDateAndName(LocalDate.of(2026, 1, 1), "New Year's Day")).isTrue();
    }

    @Test
    @DisplayName("ScheduleRepository: should persist draft schedule and query by status and teacher")
    void testScheduleRepository() {
        // Prepare Course & Section
        Course course = Course.builder()
                .courseCode("CP353001")
                .title("Database Systems")
                .weeklyHours(3)
                .build();
        entityManager.persist(course);

        Section section = Section.builder()
                .course(course)
                .sectionNumber(1)
                .capacity(40)
                .status(SectionStatus.ACTIVE)
                .build();
        entityManager.persist(section);

        // Prepare User & Teacher
        User user = User.builder()
                .universityId("T001")
                .email("teacher@kku.ac.th")
                .passwordHash("hashed")
                .role(UserRole.TEACHER)
                .build();
        entityManager.persist(user);

        Teacher teacher = Teacher.builder()
                .user(user)
                .fullName("Dr. Somchai")
                .build();
        entityManager.persist(teacher);

        // Prepare Room & TimeSlot
        Room room = Room.builder()
                .building("SC01")
                .roomNumber("201")
                .floor(2)
                .capacity(40)
                .isAvailable(true)
                .build();
        entityManager.persist(room);

        TimeSlot slot = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(16, 0))
                .build();
        entityManager.persist(slot);

        // Create Schedule
        Schedule schedule = Schedule.builder()
                .section(section)
                .teacher(teacher)
                .room(room)
                .timeSlot(slot)
                .status(ScheduleStatus.DRAFT)
                .build();
        scheduleRepository.save(schedule);

        List<Schedule> drafts = scheduleRepository.findByStatus(ScheduleStatus.DRAFT);
        assertThat(drafts).hasSize(1);

        List<Schedule> teacherSchedules = scheduleRepository.findByTeacherIdAndStatus(teacher.getId(), ScheduleStatus.DRAFT);
        assertThat(teacherSchedules).hasSize(1);

        boolean exists = scheduleRepository.existsBySectionIdAndTimeSlotId(section.getId(), slot.getId());
        assertThat(exists).isTrue();
    }
}

