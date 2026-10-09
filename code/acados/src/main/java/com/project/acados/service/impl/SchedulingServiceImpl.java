package com.project.acados.service.impl;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.repository.*;
import com.project.acados.service.ConstraintEvaluator;
import com.project.acados.service.SchedulingService;
import com.project.acados.strategy.ScoringStrategy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of SchedulingService coordinating ConstraintEvaluator and ScoringStrategy implementations.
 * Reference: Implement_Plan-AcadOS.md §7 (service/impl/), §10.2, §12
 */
@Service
@RequiredArgsConstructor
public class SchedulingServiceImpl implements SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingServiceImpl.class);
    private static final int BASELINE_SCORE = 100;
    private static final int MAX_SLOT_COMBINATIONS = 50;

    private final SectionRepository sectionRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TeacherQualificationRepository teacherQualificationRepository;
    private final ScheduleRepository scheduleRepository;
    private final ConstraintEvaluator constraintEvaluator;
    private final List<ScoringStrategy> scoringStrategies;

    private final Random random = new Random();

    @Override
    @Transactional
    public void generateSchedule() {
        log.info("Starting automated schedule generation for all active sections");
        // 1. Discard existing draft schedules
        discardDraft();

        // 2. Fetch all ACTIVE sections
        List<Section> activeSections = sectionRepository.findByStatus(SectionStatus.ACTIVE);
        List<TimeSlot> allTimeSlots = timeSlotRepository.findAll();

        if (allTimeSlots.isEmpty()) {
            log.warn("Cannot generate schedule: No TimeSlots available in database");
            return;
        }

        for (Section section : activeSections) {
            Course course = section.getCourse();
            if (course == null) {
                continue;
            }

            // Find qualified teachers
            List<TeacherQualification> qualifications = teacherQualificationRepository.findByCourseId(course.getId());
            if (qualifications.isEmpty()) {
                log.warn("No qualified teachers for course {}", course.getCourseCode());
                continue;
            }
            List<Teacher> qualifiedTeachers = qualifications.stream()
                    .map(TeacherQualification::getTeacher)
                    .filter(Objects::nonNull)
                    .toList();

            // Find eligible rooms (capacity >= section capacity, isAvailable = true)
            List<Room> eligibleRooms = roomRepository
                    .findByCapacityGreaterThanEqualAndIsAvailableTrue(section.getCapacity());
            if (eligibleRooms.isEmpty()) {
                log.warn("No available rooms for section {} (capacity {})", section.getId(), section.getCapacity());
                continue;
            }

            int requiredHours = course.getWeeklyHours() != null ? course.getWeeklyHours() : 1;
            List<List<TimeSlot>> slotCombinations = generateSlotCombinations(allTimeSlots, requiredHours);

            if (slotCombinations.isEmpty()) {
                log.warn("No valid time slot combinations for course {} (requires {} hours)",
                        course.getCourseCode(), requiredHours);
                continue;
            }

            // Evaluate valid combinations and calculate scores
            List<ScheduleCandidate> validCandidates = new ArrayList<>();

            for (Teacher teacher : qualifiedTeachers) {
                for (Room room : eligibleRooms) {
                    for (List<TimeSlot> slots : slotCombinations) {
                        boolean passed = constraintEvaluator.validate(teacher, room, section, slots);
                        if (passed) {
                            int totalScore = BASELINE_SCORE;
                            if (scoringStrategies != null) {
                                for (ScoringStrategy strategy : scoringStrategies) {
                                    totalScore += strategy.calculateScore(teacher, room, section);
                                }
                            }
                            validCandidates.add(new ScheduleCandidate(teacher, room, slots, totalScore));
                        }
                    }
                }
            }

            if (validCandidates.isEmpty()) {
                log.warn("Section {} failed scheduling: {}", section.getId(), constraintEvaluator.getLastFailureReason());
                continue;
            }

            // Select highest scoring candidate (with tie-breaking)
            ScheduleCandidate winner = selectWinner(validCandidates);
            log.info("Section {} scheduled with Teacher '{}', Room '{} {}', Score: {}",
                    section.getId(),
                    winner.teacher().getFullName(),
                    winner.room().getBuilding(),
                    winner.room().getRoomNumber(),
                    winner.score());

            // Persist as DRAFT schedules
            for (TimeSlot slot : winner.timeSlots()) {
                Schedule schedule = Schedule.builder()
                        .section(section)
                        .teacher(winner.teacher())
                        .room(winner.room())
                        .timeSlot(slot)
                        .status(ScheduleStatus.DRAFT)
                        .build();

                scheduleRepository.save(schedule);
            }
        }
    }

    @Override
    @Transactional
    public void publishSchedule() {
        log.info("Publishing all DRAFT schedules to PUBLISHED status");
        List<Schedule> draftSchedules = scheduleRepository.findByStatus(ScheduleStatus.DRAFT);
        for (Schedule schedule : draftSchedules) {
            schedule.setStatus(ScheduleStatus.PUBLISHED);
            scheduleRepository.save(schedule);
        }
        log.info("Successfully published {} schedules", draftSchedules.size());
    }

    @Override
    @Transactional
    public void discardDraft() {
        log.info("Discarding all DRAFT schedules");
        scheduleRepository.deleteByStatus(ScheduleStatus.DRAFT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Schedule> getDraftSchedules() {
        return scheduleRepository.findByStatus(ScheduleStatus.DRAFT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Schedule> getPublishedSchedules() {
        return scheduleRepository.findByStatus(ScheduleStatus.PUBLISHED);
    }

    /**
     * Selects candidate with highest score, applying random tie-breaking if scores match.
     */
    private ScheduleCandidate selectWinner(List<ScheduleCandidate> candidates) {
        int maxScore = candidates.stream()
                .mapToInt(ScheduleCandidate::score)
                .max()
                .orElse(0);

        List<ScheduleCandidate> topCandidates = candidates.stream()
                .filter(c -> c.score() == maxScore)
                .toList();

        if (topCandidates.size() == 1) {
            return topCandidates.get(0);
        }

        return topCandidates.get(random.nextInt(topCandidates.size()));
    }

    /**
     * Generates non-overlapping TimeSlot combinations of the required size.
     */
    private List<List<TimeSlot>> generateSlotCombinations(List<TimeSlot> allSlots, int count) {
        if (allSlots == null || allSlots.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }

        List<List<TimeSlot>> result = new ArrayList<>();
        findCombinationsRecursive(allSlots, count, 0, new ArrayList<>(), result);
        return result;
    }

    private void findCombinationsRecursive(
            List<TimeSlot> allSlots,
            int targetSize,
            int startIndex,
            List<TimeSlot> current,
            List<List<TimeSlot>> accumulator
    ) {
        if (accumulator.size() >= MAX_SLOT_COMBINATIONS) {
            return;
        }

        if (current.size() == targetSize) {
            accumulator.add(new ArrayList<>(current));
            return;
        }

        for (int i = startIndex; i < allSlots.size(); i++) {
            TimeSlot candidateSlot = allSlots.get(i);
            boolean overlaps = false;
            for (TimeSlot existing : current) {
                if (existing.overlapsWith(candidateSlot)) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                current.add(candidateSlot);
                findCombinationsRecursive(allSlots, targetSize, i + 1, current, accumulator);
                current.remove(current.size() - 1);
            }
        }
    }

    /**
     * Internal record representing candidate scheduling options during calculation.
     */
    private record ScheduleCandidate(Teacher teacher, Room room, List<TimeSlot> timeSlots, int score) {
    }
}

