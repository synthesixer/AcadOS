package com.project.acados.strategy;

import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Room;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.repository.TeacherPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Concrete scoring strategy: Teacher Preference (+30 points).
 * Awards 30 points if the teacher expressed preference for teaching the section's course.
 * Reference: Implement_Plan-AcadOS.md §7 (strategy/), §10.3, §12.4
 */
@Component
@RequiredArgsConstructor
public class PreferenceScoreStrategy implements ScoringStrategy {

    public static final int PREFERENCE_BONUS = 30;

    private final TeacherPreferenceRepository teacherPreferenceRepository;

    @Override
    public int calculateScore(Teacher teacher, Room room, Section section) {
        if (teacher == null || section == null || section.getCourse() == null) {
            return 0;
        }

        Course course = section.getCourse();
        boolean hasPreference = teacherPreferenceRepository
                .existsByTeacherIdAndCourseId(teacher.getId(), course.getId());

        return hasPreference ? PREFERENCE_BONUS : 0;
    }
}

