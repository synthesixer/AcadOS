package com.project.acados.service;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SchedulingService Lifecycle Integration Tests")
class SchedulingServiceIntegrationTest {

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private TeacherQualificationRepository teacherQualificationRepository;

    @Autowired
    private TeacherPreferenceRepository teacherPreferenceRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private Course course;
    private Section section;
    private Teacher teacher;
    private Room room;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();

        // 1. User & Teacher
        User user = userRepository.save(User.builder()
                .universityId("T-1001")
                .email("somchai@acados.edu")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.TEACHER)
                .build());

        teacher = teacherRepository.save(Teacher.builder()
                .user(user)
                .fullName("Prof. Somchai Srisawat")
                .build());

        // 2. Course & Section
        course = courseRepository.save(Course.builder()
                .courseCode("CP353002")
                .title("Software Architecture")
                .weeklyHours(1)
                .build());

        section = sectionRepository.save(Section.builder()
                .course(course)
                .sectionNumber(1)
                .capacity(30)
                .status(SectionStatus.ACTIVE)
                .build());

        // 3. Room
        room = roomRepository.save(Room.builder()
                .roomNumber("Lab-301")
                .building("Faculty of Science")
                .floor(3)
                .capacity(40)
                .isAvailable(true)
                .build());

        // 4. TimeSlot
        slot = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build());

        // 5. Teacher Qualification (BR-06)
        teacherQualificationRepository.save(TeacherQualification.builder()
                .teacher(teacher)
                .course(course)
                .build());

        // 6. Teacher Preference (+30 points)
        teacherPreferenceRepository.save(TeacherPreference.builder()
                .teacher(teacher)
                .course(course)
                .priority(1)
                .build());
    }

    @Test
    @DisplayName("Lifecycle Test: generateSchedule() -> publishSchedule() -> discardDraft()")
    void testCompleteSchedulingLifecycle() {
        // 1. Generate Draft Schedule
        schedulingService.generateSchedule();

        List<Schedule> drafts = schedulingService.getDraftSchedules();
        assertEquals(1, drafts.size(), "Should generate 1 draft schedule");

        Schedule draftSchedule = drafts.get(0);
        assertEquals(ScheduleStatus.DRAFT, draftSchedule.getStatus());
        assertEquals(teacher.getId(), draftSchedule.getTeacher().getId());
        assertEquals(room.getId(), draftSchedule.getRoom().getId());
        assertEquals(slot.getId(), draftSchedule.getTimeSlot().getId());

        // 2. Publish Schedule
        schedulingService.publishSchedule();

        List<Schedule> publishedSchedules = schedulingService.getPublishedSchedules();
        assertEquals(1, publishedSchedules.size());
        assertEquals(ScheduleStatus.PUBLISHED, publishedSchedules.get(0).getStatus());

        List<Schedule> remainingDrafts = schedulingService.getDraftSchedules();
        assertTrue(remainingDrafts.isEmpty(), "Draft schedules should now be empty");

        // 3. Discard Draft test
        // Add another section & slot, then generate new draft
        Section section2 = sectionRepository.save(Section.builder()
                .course(course)
                .sectionNumber(2)
                .capacity(30)
                .status(SectionStatus.ACTIVE)
                .build());

        TimeSlot slot2 = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(14, 0))
                .build());

        schedulingService.generateSchedule();
        List<Schedule> newDrafts = schedulingService.getDraftSchedules();
        assertFalse(newDrafts.isEmpty(), "New draft schedules should be present");

        schedulingService.discardDraft();
        List<Schedule> afterDiscard = schedulingService.getDraftSchedules();
        assertTrue(afterDiscard.isEmpty(), "Draft schedules must be completely deleted after discardDraft");

        // Published schedules must not be affected
        List<Schedule> stillPublished = schedulingService.getPublishedSchedules();
        assertEquals(1, stillPublished.size(), "Previously published schedules must remain untouched");
    }

    @Test
    @DisplayName("Support 2 sessions per week (4 hours total, 2 hours per session)")
    void testMultiSessionTwoHourScheduling() {
        // Deactivate setup section to test English section independently
        section.setStatus(SectionStatus.CANCELLED);
        sectionRepository.save(section);

        // Course: English (4 hours weekly)
        Course englishCourse = courseRepository.save(Course.builder()
                .courseCode("EN012001")
                .title("Technical English for Computing")
                .weeklyHours(4)
                .build());

        Section englishSection = sectionRepository.save(Section.builder()
                .course(englishCourse)
                .sectionNumber(1)
                .capacity(30)
                .status(SectionStatus.ACTIVE)
                .build());

        teacherQualificationRepository.save(TeacherQualification.builder()
                .teacher(teacher)
                .course(englishCourse)
                .build());

        // Create two 2-hour slots: Mon 09:00-11:00 and Wed 09:00-11:00
        timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .build());

        timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .build());

        schedulingService.generateSchedule();

        List<Schedule> drafts = schedulingService.getDraftSchedules();
        assertEquals(2, drafts.size(), "Should generate exactly 2 draft schedules for 2 sessions per week");

        long totalHours = drafts.stream()
                .mapToLong(s -> java.time.Duration.between(s.getTimeSlot().getStartTime(), s.getTimeSlot().getEndTime()).toHours())
                .sum();
        assertEquals(4, totalHours, "Total scheduled hours across 2 sessions must equal 4 hours");

        List<DayOfWeek> days = drafts.stream().map(s -> s.getTimeSlot().getDayOfWeek()).toList();
        assertTrue(days.contains(DayOfWeek.MONDAY));
        assertTrue(days.contains(DayOfWeek.WEDNESDAY));
    }
}

