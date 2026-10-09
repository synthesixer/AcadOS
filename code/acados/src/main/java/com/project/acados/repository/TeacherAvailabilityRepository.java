package com.project.acados.repository;

import com.project.acados.domain.entity.TeacherAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TeacherAvailability entities.
 * Reference: database.md §2.12, class diagram.puml
 */
@Repository
public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, Long> {

    List<TeacherAvailability> findByTeacherId(Long teacherId);

    Optional<TeacherAvailability> findByTeacherIdAndTimeSlotId(Long teacherId, Long timeSlotId);

    boolean existsByTeacherIdAndTimeSlotId(Long teacherId, Long timeSlotId);
}
