package com.project.acados.strategy;

import com.project.acados.domain.entity.Room;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Concrete scoring strategy: Workload Balance (+20 points).
 * Awards 20 points if assigning this section keeps the teacher's workload balanced.
 * Reference: Implement_Plan-AcadOS.md §7 (strategy/), §10.3, §12.4
 */
@Component
@RequiredArgsConstructor
public class WorkloadScoreStrategy implements ScoringStrategy {

    public static final int WORKLOAD_BONUS = 20;
    public static final int BALANCED_WORKLOAD_THRESHOLD = 12; // periods per week

    private final ScheduleRepository scheduleRepository;

    @Override
    public int calculateScore(Teacher teacher, Room room, Section section) {
        if (teacher == null) {
            return 0;
        }

        long currentWorkload = scheduleRepository.countByTeacherId(teacher.getId());

        if (currentWorkload <= BALANCED_WORKLOAD_THRESHOLD) {
            return WORKLOAD_BONUS;
        }

        return 0;
    }
}

