package com.project.acados.repository;

import com.project.acados.domain.entity.TeacherSwapRequest;
import com.project.acados.domain.enums.SwapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for TeacherSwapRequest entities.
 * Supports audit lookups and double open swap prevention (Risk 2).
 * Reference: database.md §2.13, class diagram.puml
 */
@Repository
public interface TeacherSwapRequestRepository extends JpaRepository<TeacherSwapRequest, Long> {

    List<TeacherSwapRequest> findByStatus(SwapStatus status);

    List<TeacherSwapRequest> findByRequestingTeacherId(Long teacherId);

    List<TeacherSwapRequest> findByTargetTeacherId(Long teacherId);

    @Query("SELECT r FROM TeacherSwapRequest r WHERE r.requestingTeacher.id = :teacherId OR r.targetTeacher.id = :teacherId")
    List<TeacherSwapRequest> findByTeacherInvolved(@Param("teacherId") Long teacherId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM TeacherSwapRequest r " +
           "WHERE (r.requestingSchedule.id = :scheduleId OR r.targetSchedule.id = :scheduleId) " +
           "AND r.status IN :statuses")
    boolean existsOpenSwapForSchedule(@Param("scheduleId") Long scheduleId, @Param("statuses") Collection<SwapStatus> statuses);
}

