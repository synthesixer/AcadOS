package com.project.acados.strategy;

import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.repository.TeacherPreferenceRepository;
import com.project.acados.service.Candidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Concrete scoring strategy: Teacher Preference (+30 points).
 * Awards 30 points if the teacher expressed preference for teaching the section's course.
 * Reference: class diagram.puml (§6 strategy), Implement_Plan-AcadOS.md §7 (strategy/), §10.3, §12.4
 */
@Component
@RequiredArgsConstructor
public class PreferenceScoreStrategy implements ScoringStrategy {

    public static final int PREFERENCE_BONUS = 30;

    private final TeacherPreferenceRepository teacherPreferenceRepository;

    @Override
    public int calculateScore(Candidate candidate) {
        if (candidate == null || candidate.getTeacher() == null || candidate.getSection() == null) {
            return 0;
        }

        Teacher teacher = candidate.getTeacher();
        Course course = candidate.getSection().getCourse();
        if (course == null) {
            return 0;
        }

        boolean hasPreference = teacherPreferenceRepository
                .existsByTeacherIdAndCourseId(teacher.getId(), course.getId());

        return hasPreference ? PREFERENCE_BONUS : 0;
    }
}
