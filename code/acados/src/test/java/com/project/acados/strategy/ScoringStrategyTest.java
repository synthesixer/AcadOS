package com.project.acados.strategy;

import com.project.acados.domain.entity.*;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.TeacherPreferenceRepository;
import com.project.acados.service.Candidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringStrategy Pattern Unit Tests (Class Diagram Spec)")
class ScoringStrategyTest {

    @Mock
    private TeacherPreferenceRepository teacherPreferenceRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private PreferenceScoreStrategy preferenceScoreStrategy;

    @InjectMocks
    private WorkloadScoreStrategy workloadScoreStrategy;

    private final RoomSuitabilityScoreStrategy roomSuitabilityScoreStrategy = new RoomSuitabilityScoreStrategy();

    @Test
    @DisplayName("PreferenceScoreStrategy: should award +30 points when preference exists")
    void preferenceShouldAward30WhenExists() {
        Course course = Course.builder().id(1L).build();
        Section section = Section.builder().id(10L).course(course).build();
        Teacher teacher = Teacher.builder().id(20L).build();
        Candidate candidate = Candidate.builder().section(section).teacher(teacher).build();

        when(teacherPreferenceRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(true);

        int score = preferenceScoreStrategy.calculateScore(candidate);
        assertEquals(30, score);
    }

    @Test
    @DisplayName("PreferenceScoreStrategy: should return 0 points when no preference")
    void preferenceShouldReturn0WhenNone() {
        Course course = Course.builder().id(1L).build();
        Section section = Section.builder().id(10L).course(course).build();
        Teacher teacher = Teacher.builder().id(20L).build();
        Candidate candidate = Candidate.builder().section(section).teacher(teacher).build();

        when(teacherPreferenceRepository.existsByTeacherIdAndCourseId(20L, 1L)).thenReturn(false);

        int score = preferenceScoreStrategy.calculateScore(candidate);
        assertEquals(0, score);
    }

    @Test
    @DisplayName("WorkloadScoreStrategy: should award +20 points when workload <= threshold")
    void workloadShouldAward20WhenBalanced() {
        Teacher teacher = Teacher.builder().id(20L).build();
        Candidate candidate = Candidate.builder().teacher(teacher).build();

        when(scheduleRepository.countByTeacherId(20L)).thenReturn(8L);

        int score = workloadScoreStrategy.calculateScore(candidate);
        assertEquals(20, score);
    }

    @Test
    @DisplayName("WorkloadScoreStrategy: should return 0 points when workload exceeds threshold")
    void workloadShouldReturn0WhenOverloaded() {
        Teacher teacher = Teacher.builder().id(20L).build();
        Candidate candidate = Candidate.builder().teacher(teacher).build();

        when(scheduleRepository.countByTeacherId(20L)).thenReturn(16L);

        int score = workloadScoreStrategy.calculateScore(candidate);
        assertEquals(0, score);
    }

    @Test
    @DisplayName("RoomSuitabilityScoreStrategy: should award +20 points for good capacity fit")
    void roomSuitabilityShouldAward20ForGoodFit() {
        Section section = Section.builder().capacity(40).build();
        Room room = Room.builder().capacity(45).build();
        Candidate candidate = Candidate.builder().section(section).room(room).build();

        int score = roomSuitabilityScoreStrategy.calculateScore(candidate);
        assertEquals(20, score);
    }

    @Test
    @DisplayName("RoomSuitabilityScoreStrategy: should return 0 points for oversized room")
    void roomSuitabilityShouldReturn0ForOversizedRoom() {
        Section section = Section.builder().capacity(20).build();
        Room room = Room.builder().capacity(80).build(); // 80 > 20 * 1.5, excess > 15
        Candidate candidate = Candidate.builder().section(section).room(room).build();

        int score = roomSuitabilityScoreStrategy.calculateScore(candidate);
        assertEquals(0, score);
    }
}
