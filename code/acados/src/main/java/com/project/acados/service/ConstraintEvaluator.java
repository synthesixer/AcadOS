package com.project.acados.service;

import com.project.acados.domain.entity.*;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.TeacherAvailabilityRepository;
import com.project.acados.repository.TeacherQualificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Evaluates candidate schedule assignments against the 7 Hard Constraints defined in §12.3.
 * All hard constraints must pass before a candidate can be assigned or scored.
 * Reference: class diagram.puml (§6 scheduling), Implement_Plan-AcadOS.md §10.3, §12.3
 */
@Component
@RequiredArgsConstructor
public class ConstraintEvaluator {

    private final TeacherQualificationRepository teacherQualificationRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final ScheduleRepository scheduleRepository;

    /**
     * Validates a candidate against all 7 Hard Constraints according to class diagram.puml.
     *
     * @param candidate candidate containing teacher, room, section, and timeSlots
     * @return ValidationResult with passed status and failure reason if rejected
     */
    public ValidationResult validate(Candidate candidate) {
        if (candidate == null) {
            return ValidationResult.fail("Candidate cannot be null");
        }

        Section section = candidate.getSection();
        Teacher teacher = candidate.getTeacher();
        Room room = candidate.getRoom();
        List<TimeSlot> timeSlots = candidate.getTimeSlots();

        if (section == null || section.getCourse() == null) {
            return ValidationResult.fail("Section and associated course cannot be null");
        }
        if (teacher == null) {
            return ValidationResult.fail("Teacher cannot be null");
        }
        if (room == null) {
            return ValidationResult.fail("Room cannot be null");
        }
        if (timeSlots == null || timeSlots.isEmpty()) {
            return ValidationResult.fail("Time slots cannot be empty");
        }

        // 1. Hard Constraint 1: Teacher Qualification (BR-06)
        ValidationResult qualResult = validateTeacherQualification(teacher, section.getCourse());
        if (!qualResult.isPassed()) {
            return qualResult;
        }

        // 2. Hard Constraint 2: Teacher Availability (BR-07)
        ValidationResult availResult = validateTeacherAvailability(teacher, timeSlots);
        if (!availResult.isPassed()) {
            return availResult;
        }

        // 3. Hard Constraint 3: Teacher Time Conflict (BR-01)
        ValidationResult teacherConflictResult = validateTeacherConflict(teacher, section, timeSlots);
        if (!teacherConflictResult.isPassed()) {
            return teacherConflictResult;
        }

        // 4. Hard Constraint 4: Room Time Conflict (BR-02)
        ValidationResult roomConflictResult = validateRoomConflict(room, section, timeSlots);
        if (!roomConflictResult.isPassed()) {
            return roomConflictResult;
        }

        // 5. Hard Constraint 5: Room Availability (BR-08)
        ValidationResult roomAvailResult = validateRoomAvailability(room);
        if (!roomAvailResult.isPassed()) {
            return roomAvailResult;
        }

        // 6. Hard Constraint 6: Section / Student Conflict (BR-03)
        ValidationResult sectionConflictResult = validateSectionInternalConflict(section, timeSlots);
        if (!sectionConflictResult.isPassed()) {
            return sectionConflictResult;
        }

        // 7. Hard Constraint 7: Room Capacity (BR-05)
        ValidationResult capacityResult = validateRoomCapacity(room, section);
        if (!capacityResult.isPassed()) {
            return capacityResult;
        }

        return ValidationResult.pass();
    }

    /**
     * BR-06: Teacher must have qualification for the course.
     */
    private ValidationResult validateTeacherQualification(Teacher teacher, Course course) {
        boolean qualified = teacherQualificationRepository
                .existsByTeacherIdAndCourseId(teacher.getId(), course.getId());
        if (!qualified) {
            return ValidationResult.fail(String.format(
                    "BR-06: Teacher '%s' lacks qualification for course '%s'",
                    teacher.getFullName(), course.getCourseCode()
            ));
        }
        return ValidationResult.pass();
    }

    /**
     * BR-07: Teacher must not be marked unavailable (is_available = false) during any time slot.
     */
    private ValidationResult validateTeacherAvailability(Teacher teacher, List<TimeSlot> timeSlots) {
        for (TimeSlot slot : timeSlots) {
            Optional<TeacherAvailability> availOpt = teacherAvailabilityRepository
                    .findByTeacherIdAndTimeSlotId(teacher.getId(), slot.getId());
            if (availOpt.isPresent() && !availOpt.get().isAvailable()) {
                return ValidationResult.fail(String.format(
                        "BR-07: Teacher '%s' is marked unavailable on %s %s-%s",
                        teacher.getFullName(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()
                ));
            }
        }
        return ValidationResult.pass();
    }

    /**
     * BR-01: Teacher cannot have overlapping teaching schedules (DRAFT or PUBLISHED).
     */
    private ValidationResult validateTeacherConflict(Teacher teacher, Section section, List<TimeSlot> timeSlots) {
        List<Schedule> existingSchedules = scheduleRepository.findByTeacherId(teacher.getId());
        for (Schedule existing : existingSchedules) {
            if (existing.getSection() != null && existing.getSection().getId().equals(section.getId())) {
                continue;
            }
            for (TimeSlot candidateSlot : timeSlots) {
                if (existing.getTimeSlot() != null && existing.getTimeSlot().overlapsWith(candidateSlot)) {
                    return ValidationResult.fail(String.format(
                            "BR-01: Teacher '%s' has conflicting schedule on %s %s-%s",
                            teacher.getFullName(), candidateSlot.getDayOfWeek(),
                            candidateSlot.getStartTime(), candidateSlot.getEndTime()
                    ));
                }
            }
        }
        return ValidationResult.pass();
    }

    /**
     * BR-02: Room cannot host overlapping sections (DRAFT or PUBLISHED).
     */
    private ValidationResult validateRoomConflict(Room room, Section section, List<TimeSlot> timeSlots) {
        List<Schedule> existingSchedules = scheduleRepository.findByRoomId(room.getId());
        for (Schedule existing : existingSchedules) {
            if (existing.getSection() != null && existing.getSection().getId().equals(section.getId())) {
                continue;
            }
            for (TimeSlot candidateSlot : timeSlots) {
                if (existing.getTimeSlot() != null && existing.getTimeSlot().overlapsWith(candidateSlot)) {
                    return ValidationResult.fail(String.format(
                            "BR-02: Room '%s %s' has conflicting schedule on %s %s-%s",
                            room.getBuilding(), room.getRoomNumber(),
                            candidateSlot.getDayOfWeek(), candidateSlot.getStartTime(), candidateSlot.getEndTime()
                    ));
                }
            }
        }
        return ValidationResult.pass();
    }

    /**
     * BR-08: Room must be active and available.
     */
    private ValidationResult validateRoomAvailability(Room room) {
        if (!Boolean.TRUE.equals(room.getIsAvailable())) {
            return ValidationResult.fail(String.format(
                    "BR-08: Room '%s %s' is not available",
                    room.getBuilding(), room.getRoomNumber()
            ));
        }
        return ValidationResult.pass();
    }

    /**
     * BR-03: Section time slots must not overlap internally or with existing section schedules.
     */
    private ValidationResult validateSectionInternalConflict(Section section, List<TimeSlot> timeSlots) {
        // Internal overlap within candidate slots
        for (int i = 0; i < timeSlots.size(); i++) {
            for (int j = i + 1; j < timeSlots.size(); j++) {
                if (timeSlots.get(i).overlapsWith(timeSlots.get(j))) {
                    return ValidationResult.fail(String.format(
                            "BR-03: Candidate time slots overlap each other internally (%s %s-%s and %s %s-%s)",
                            timeSlots.get(i).getDayOfWeek(), timeSlots.get(i).getStartTime(), timeSlots.get(i).getEndTime(),
                            timeSlots.get(j).getDayOfWeek(), timeSlots.get(j).getStartTime(), timeSlots.get(j).getEndTime()
                    ));
                }
            }
        }

        // Overlap with existing schedules of this section
        List<Schedule> sectionSchedules = scheduleRepository.findBySectionId(section.getId());
        for (Schedule existing : sectionSchedules) {
            for (TimeSlot candidateSlot : timeSlots) {
                if (existing.getTimeSlot() != null && existing.getTimeSlot().overlapsWith(candidateSlot)) {
                    return ValidationResult.fail(String.format(
                            "BR-03: Section already has scheduled period on %s %s-%s",
                            candidateSlot.getDayOfWeek(), candidateSlot.getStartTime(), candidateSlot.getEndTime()
                    ));
                }
            }
        }

        return ValidationResult.pass();
    }

    /**
     * BR-05: Room capacity must be greater than or equal to section capacity.
     */
    private ValidationResult validateRoomCapacity(Room room, Section section) {
        if (room.getCapacity() < section.getCapacity()) {
            return ValidationResult.fail(String.format(
                    "BR-05: Room capacity (%d) is insufficient for section capacity (%d)",
                    room.getCapacity(), section.getCapacity()
            ));
        }
        return ValidationResult.pass();
    }
}
