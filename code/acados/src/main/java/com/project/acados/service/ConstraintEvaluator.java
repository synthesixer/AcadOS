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
 * Reference: Implement_Plan-AcadOS.md §10.3, §12.3
 */
@Component
@RequiredArgsConstructor
public class ConstraintEvaluator {

    private final TeacherQualificationRepository teacherQualificationRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final ScheduleRepository scheduleRepository;

    private String lastFailureReason;

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    /**
     * Validates candidate parameters against all 7 Hard Constraints.
     *
     * @param teacher candidate teacher
     * @param room candidate room
     * @param section section being scheduled
     * @param timeSlots list of periods for the section
     * @return true if all 7 hard constraints pass; false otherwise
     */
    public boolean validate(Teacher teacher, Room room, Section section, List<TimeSlot> timeSlots) {
        lastFailureReason = null;

        if (section == null || section.getCourse() == null) {
            lastFailureReason = "Section and associated course cannot be null";
            return false;
        }
        if (teacher == null) {
            lastFailureReason = "Teacher cannot be null";
            return false;
        }
        if (room == null) {
            lastFailureReason = "Room cannot be null";
            return false;
        }
        if (timeSlots == null || timeSlots.isEmpty()) {
            lastFailureReason = "Time slots cannot be empty";
            return false;
        }

        // 1. Hard Constraint 1: Teacher Qualification (BR-06)
        if (!validateTeacherQualification(teacher, section.getCourse())) {
            return false;
        }

        // 2. Hard Constraint 2: Teacher Availability (BR-07)
        if (!validateTeacherAvailability(teacher, timeSlots)) {
            return false;
        }

        // 3. Hard Constraint 3: Teacher Time Conflict (BR-01)
        if (!validateTeacherConflict(teacher, section, timeSlots)) {
            return false;
        }

        // 4. Hard Constraint 4: Room Time Conflict (BR-02)
        if (!validateRoomConflict(room, section, timeSlots)) {
            return false;
        }

        // 5. Hard Constraint 5: Room Availability (BR-08)
        if (!validateRoomAvailability(room)) {
            return false;
        }

        // 6. Hard Constraint 6: Section / Student Conflict (BR-03)
        if (!validateSectionInternalConflict(section, timeSlots)) {
            return false;
        }

        // 7. Hard Constraint 7: Room Capacity (BR-05)
        if (!validateRoomCapacity(room, section)) {
            return false;
        }

        return true;
    }

    /**
     * Convenience overload for a single TimeSlot.
     */
    public boolean validate(Teacher teacher, Room room, Section section, TimeSlot timeSlot) {
        if (timeSlot == null) {
            lastFailureReason = "Time slot cannot be null";
            return false;
        }
        return validate(teacher, room, section, List.of(timeSlot));
    }

    /**
     * BR-06: Teacher must have qualification for the course.
     */
    private boolean validateTeacherQualification(Teacher teacher, Course course) {
        boolean qualified = teacherQualificationRepository
                .existsByTeacherIdAndCourseId(teacher.getId(), course.getId());
        if (!qualified) {
            lastFailureReason = String.format(
                    "BR-06: Teacher '%s' lacks qualification for course '%s'",
                    teacher.getFullName(), course.getCourseCode()
            );
            return false;
        }
        return true;
    }

    /**
     * BR-07: Teacher must not be marked unavailable (is_available = false) during any time slot.
     */
    private boolean validateTeacherAvailability(Teacher teacher, List<TimeSlot> timeSlots) {
        for (TimeSlot slot : timeSlots) {
            Optional<TeacherAvailability> availOpt = teacherAvailabilityRepository
                    .findByTeacherIdAndTimeSlotId(teacher.getId(), slot.getId());
            if (availOpt.isPresent() && !availOpt.get().isAvailable()) {
                lastFailureReason = String.format(
                        "BR-07: Teacher '%s' is marked unavailable on %s %s-%s",
                        teacher.getFullName(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()
                );
                return false;
            }
        }
        return true;
    }

    /**
     * BR-01: Teacher cannot have overlapping teaching schedules (DRAFT or PUBLISHED).
     */
    private boolean validateTeacherConflict(Teacher teacher, Section section, List<TimeSlot> timeSlots) {
        List<Schedule> existingSchedules = scheduleRepository.findByTeacherId(teacher.getId());
        for (Schedule existing : existingSchedules) {
            if (existing.getSection() != null && existing.getSection().getId().equals(section.getId())) {
                continue;
            }
            for (TimeSlot candidateSlot : timeSlots) {
                if (existing.getTimeSlot() != null && existing.getTimeSlot().overlapsWith(candidateSlot)) {
                    lastFailureReason = String.format(
                            "BR-01: Teacher '%s' has conflicting schedule on %s %s-%s",
                            teacher.getFullName(), candidateSlot.getDayOfWeek(),
                            candidateSlot.getStartTime(), candidateSlot.getEndTime()
                    );
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * BR-02: Room cannot host overlapping sections (DRAFT or PUBLISHED).
     */
    private boolean validateRoomConflict(Room room, Section section, List<TimeSlot> timeSlots) {
        List<Schedule> existingSchedules = scheduleRepository.findByRoomId(room.getId());
        for (Schedule existing : existingSchedules) {
            if (existing.getSection() != null && existing.getSection().getId().equals(section.getId())) {
                continue;
            }
            for (TimeSlot candidateSlot : timeSlots) {
                if (existing.getTimeSlot() != null && existing.getTimeSlot().overlapsWith(candidateSlot)) {
                    lastFailureReason = String.format(
                            "BR-02: Room '%s %s' has conflicting schedule on %s %s-%s",
                            room.getBuilding(), room.getRoomNumber(),
                            candidateSlot.getDayOfWeek(), candidateSlot.getStartTime(), candidateSlot.getEndTime()
                    );
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * BR-08: Room must be active and available.
     */
    private boolean validateRoomAvailability(Room room) {
        if (!Boolean.TRUE.equals(room.getIsAvailable())) {
            lastFailureReason = String.format(
                    "BR-08: Room '%s %s' is not available",
                    room.getBuilding(), room.getRoomNumber()
            );
            return false;
        }
        return true;
    }

    /**
     * BR-03: Section time slots must not overlap internally or with existing section schedules.
     */
    private boolean validateSectionInternalConflict(Section section, List<TimeSlot> timeSlots) {
        // Internal overlap within the candidate slots
        for (int i = 0; i < timeSlots.size(); i++) {
            for (int j = i + 1; j < timeSlots.size(); j++) {
                if (timeSlots.get(i).overlapsWith(timeSlots.get(j))) {
                    lastFailureReason = String.format(
                            "BR-03: Section periods overlap internally (%s %s-%s and %s %s-%s)",
                            timeSlots.get(i).getDayOfWeek(), timeSlots.get(i).getStartTime(), timeSlots.get(i).getEndTime(),
                            timeSlots.get(j).getDayOfWeek(), timeSlots.get(j).getStartTime(), timeSlots.get(j).getEndTime()
                    );
                    return false;
                }
            }
        }

        // Overlap with existing schedules of this section
        List<Schedule> sectionSchedules = scheduleRepository.findBySectionId(section.getId());
        for (Schedule existing : sectionSchedules) {
            for (TimeSlot candidateSlot : timeSlots) {
                if (existing.getTimeSlot() != null && existing.getTimeSlot().overlapsWith(candidateSlot)) {
                    lastFailureReason = String.format(
                            "BR-03: Section already has scheduled period on %s %s-%s",
                            candidateSlot.getDayOfWeek(), candidateSlot.getStartTime(), candidateSlot.getEndTime()
                    );
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * BR-05: Room capacity must be greater than or equal to section capacity.
     */
    private boolean validateRoomCapacity(Room room, Section section) {
        if (room.getCapacity() < section.getCapacity()) {
            lastFailureReason = String.format(
                    "BR-05: Room capacity (%d) is insufficient for section capacity (%d)",
                    room.getCapacity(), section.getCapacity()
            );
            return false;
        }
        return true;
    }
}

