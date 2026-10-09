package com.project.acados.service;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.TeacherAvailabilityRepository;
import com.project.acados.repository.TeacherQualificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConstraintEvaluator Unit Tests for 7 Hard Constraints")
class ConstraintEvaluatorTest {

    @Mock
    private TeacherQualificationRepository teacherQualificationRepository;

    @Mock
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ConstraintEvaluator evaluator;

    private Course course;
    private Section section;
    private Teacher teacher;
    private Room room;
    private TimeSlot timeSlot1;
    private TimeSlot timeSlot2;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(1L)
                .courseCode("CP353002")
                .title("Software Design")
                .weeklyHours(2)
                .build();

        section = Section.builder()
                .id(10L)
                .course(course)
                .sectionNumber(1)
                .capacity(40)
                .status(SectionStatus.ACTIVE)
                .build();

        teacher = Teacher.builder()
                .id(20L)
                .fullName("Dr. Somchai Srisawat")
                .build();

        room = Room.builder()
                .id(30L)
                .building("SC-01")
                .roomNumber("401")
                .floor(4)
                .capacity(50)
                .isAvailable(true)
                .build();

        timeSlot1 = TimeSlot.builder()
                .id(101L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();

        timeSlot2 = TimeSlot.builder()
                .id(102L)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
    }

    @Test
    @DisplayName("Should pass when all 7 hard constraints are satisfied")
    void shouldPassWhenAllConstraintsSatisfied() {
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(true);
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(20L, 101L)).thenReturn(Optional.empty());
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(20L, 102L)).thenReturn(Optional.empty());
        when(scheduleRepository.findByTeacherId(20L)).thenReturn(Collections.emptyList());
        when(scheduleRepository.findByRoomId(30L)).thenReturn(Collections.emptyList());
        when(scheduleRepository.findBySectionId(10L)).thenReturn(Collections.emptyList());

        boolean passed = evaluator.validate(teacher, room, section, List.of(timeSlot1, timeSlot2));

        assertTrue(passed);
        assertNull(evaluator.getLastFailureReason());
    }

    @Test
    @DisplayName("BR-06: Should fail when teacher lacks qualification")
    void shouldFailWhenTeacherNotQualified() {
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(false);

        boolean passed = evaluator.validate(teacher, room, section, List.of(timeSlot1));

        assertFalse(passed);
        assertNotNull(evaluator.getLastFailureReason());
        assertTrue(evaluator.getLastFailureReason().contains("BR-06"));
    }

    @Test
    @DisplayName("BR-07: Should fail when teacher is marked unavailable")
    void shouldFailWhenTeacherUnavailable() {
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(true);

        TeacherAvailability unavailable = TeacherAvailability.builder()
                .teacher(teacher)
                .timeSlot(timeSlot1)
                .isAvailable(false)
                .build();

        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(20L, 101L))
                .thenReturn(Optional.of(unavailable));

        boolean passed = evaluator.validate(teacher, room, section, List.of(timeSlot1));

        assertFalse(passed);
        assertNotNull(evaluator.getLastFailureReason());
        assertTrue(evaluator.getLastFailureReason().contains("BR-07"));
    }

    @Test
    @DisplayName("BR-01: Should fail when teacher has overlapping schedule")
    void shouldFailWhenTeacherHasConflict() {
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(true);
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(20L, 101L)).thenReturn(Optional.empty());

        Section otherSection = Section.builder().id(99L).build();
        Schedule conflictSchedule = Schedule.builder()
                .id(501L)
                .teacher(teacher)
                .section(otherSection)
                .timeSlot(timeSlot1)
                .status(ScheduleStatus.PUBLISHED)
                .build();

        when(scheduleRepository.findByTeacherId(20L)).thenReturn(List.of(conflictSchedule));

        boolean passed = evaluator.validate(teacher, room, section, List.of(timeSlot1));

        assertFalse(passed);
        assertNotNull(evaluator.getLastFailureReason());
        assertTrue(evaluator.getLastFailureReason().contains("BR-01"));
    }

    @Test
    @DisplayName("BR-02: Should fail when room has overlapping schedule")
    void shouldFailWhenRoomHasConflict() {
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(true);
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(20L, 101L)).thenReturn(Optional.empty());
        when(scheduleRepository.findByTeacherId(20L)).thenReturn(Collections.emptyList());

        Section otherSection = Section.builder().id(99L).build();
        Schedule conflictSchedule = Schedule.builder()
                .id(502L)
                .room(room)
                .section(otherSection)
                .timeSlot(timeSlot1)
                .status(ScheduleStatus.DRAFT)
                .build();

        when(scheduleRepository.findByRoomId(30L)).thenReturn(List.of(conflictSchedule));

        boolean passed = evaluator.validate(teacher, room, section, List.of(timeSlot1));

        assertFalse(passed);
        assertNotNull(evaluator.getLastFailureReason());
        assertTrue(evaluator.getLastFailureReason().contains("BR-02"));
    }

    @Test
    @DisplayName("BR-08: Should fail when room is not available")
    void shouldFailWhenRoomNotAvailable() {
        room.setIsAvailable(false);

        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(true);
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(20L, 101L)).thenReturn(Optional.empty());
        when(scheduleRepository.findByTeacherId(20L)).thenReturn(Collections.emptyList());
        when(scheduleRepository.findByRoomId(30L)).thenReturn(Collections.emptyList());

        boolean passed = evaluator.validate(teacher, room, section, List.of(timeSlot1));

        assertFalse(passed);
        assertNotNull(evaluator.getLastFailureReason());
        assertTrue(evaluator.getLastFailureReason().contains("BR-08"));
    }

    @Test
    @DisplayName("BR-05: Should fail when room capacity is insufficient for section")
    void shouldFailWhenRoomCapacityInsufficient() {
        room.setCapacity(30); // Section capacity is 40

        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(true);
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(20L, 101L)).thenReturn(Optional.empty());
        when(scheduleRepository.findByTeacherId(20L)).thenReturn(Collections.emptyList());
        when(scheduleRepository.findByRoomId(30L)).thenReturn(Collections.emptyList());
        when(scheduleRepository.findBySectionId(10L)).thenReturn(Collections.emptyList());

        boolean passed = evaluator.validate(teacher, room, section, List.of(timeSlot1));

        assertFalse(passed);
        assertNotNull(evaluator.getLastFailureReason());
        assertTrue(evaluator.getLastFailureReason().contains("BR-05"));
    }

    @Test
    @DisplayName("BR-03: Should fail when candidate periods overlap internally")
    void shouldFailWhenCandidateSlotsOverlapInternally() {
        TimeSlot overlappingSlot = TimeSlot.builder()
                .id(103L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(11, 0))
                .build();

        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(true);
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(scheduleRepository.findByTeacherId(20L)).thenReturn(Collections.emptyList());
        when(scheduleRepository.findByRoomId(30L)).thenReturn(Collections.emptyList());

        boolean passed = evaluator.validate(teacher, room, section, List.of(timeSlot1, overlappingSlot));

        assertFalse(passed);
        assertNotNull(evaluator.getLastFailureReason());
        assertTrue(evaluator.getLastFailureReason().contains("BR-03"));
    }
}

