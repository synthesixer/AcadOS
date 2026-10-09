package com.project.acados.strategy;

import com.project.acados.domain.entity.Teacher;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.service.Candidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Concrete scoring strategy: Workload Balance (+20 points).
 * Awards 20 points if assigning this section keeps the teacher's workload balanced.
 * Reference: class diagram.puml (§6 strategy), Implement_Plan-AcadOS.md §7 (strategy/), §10.3, §12.4
 *
 * NOTE [INFERRED]:
 * เกณฑ์กระจายภาระงานสอน (+20 คะแนน) ใน Implement_Plan-AcadOS.md §12.4 ระบุวัตถุประสงค์
 * เพื่อกระจายภาระงานสอนของอาจารย์ในภาควิชาให้สมดุล ไม่กระจุกตัว แต่เอกสารไม่ได้ระบุตัวเลขชั่วโมงสูงสุด
 * ปัจจุบันอนุมานเกณฑ์: BALANCED_WORKLOAD_THRESHOLD = 12 คาบ/สัปดาห์
 * ทำการ noted ไว้ในโค้ด เผื่ออัปเดตตามนโยบายภาควิชาในอนาคต
 */
@Component
@RequiredArgsConstructor
public class WorkloadScoreStrategy implements ScoringStrategy {

    public static final int WORKLOAD_BONUS = 20;

    /**
     * NOTE [INFERRED]: Threshold กำหนดไว้ที่ 12 คาบ/สัปดาห์ เผื่ออัปเดตในอนาคต
     */
    public static final int BALANCED_WORKLOAD_THRESHOLD = 12; // periods per week

    private final ScheduleRepository scheduleRepository;

    @Override
    public int calculateScore(Candidate candidate) {
        if (candidate == null || candidate.getTeacher() == null) {
            return 0;
        }

        Teacher teacher = candidate.getTeacher();
        long currentWorkload = scheduleRepository.countByTeacherId(teacher.getId());

        if (currentWorkload <= BALANCED_WORKLOAD_THRESHOLD) {
            return WORKLOAD_BONUS;
        }

        return 0;
    }
}
