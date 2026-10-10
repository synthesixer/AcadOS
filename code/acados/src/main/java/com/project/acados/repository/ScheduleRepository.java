package com.project.acados.repository;

import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Schedule entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §7, database.md §2.8
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @EntityGraph(attributePaths = {"section", "section.course", "teacher", "room", "timeSlot"})
    List<Schedule> findByStatus(ScheduleStatus status);

    @EntityGraph(attributePaths = {"section", "section.course", "teacher", "room", "timeSlot"})
    List<Schedule> findBySectionId(Long sectionId);

    @EntityGraph(attributePaths = {"section", "section.course", "teacher", "room", "timeSlot"})
    List<Schedule> findByTeacherId(Long teacherId);

    @EntityGraph(attributePaths = {"section", "section.course", "teacher", "room", "timeSlot"})
    List<Schedule> findByTeacherIdAndStatus(Long teacherId, ScheduleStatus status);

    @EntityGraph(attributePaths = {"section", "section.course", "teacher", "room", "timeSlot"})
    List<Schedule> findByRoomId(Long roomId);

    @EntityGraph(attributePaths = {"section", "section.course", "teacher", "room", "timeSlot"})
    List<Schedule> findByRoomIdAndStatus(Long roomId, ScheduleStatus status);

    long countByTeacherId(Long teacherId);

    boolean existsBySectionIdAndTimeSlotId(Long sectionId, Long timeSlotId);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.status = :status")
    void deleteByStatus(@Param("status") ScheduleStatus status);

    @Query("SELECT s FROM Schedule s WHERE s.teacher.id = :teacherId AND s.timeSlot.id = :timeSlotId AND s.status = :status")
    List<Schedule> findByTeacherAndTimeSlotAndStatus(
            @Param("teacherId") Long teacherId,
            @Param("timeSlotId") Long timeSlotId,
            @Param("status") ScheduleStatus status
    );

    @Query("SELECT s FROM Schedule s WHERE s.room.id = :roomId AND s.timeSlot.id = :timeSlotId AND s.status = :status")
    List<Schedule> findByRoomAndTimeSlotAndStatus(
            @Param("roomId") Long roomId,
            @Param("timeSlotId") Long timeSlotId,
            @Param("status") ScheduleStatus status
    );
}

