package com.project.acados.repository;

import com.project.acados.domain.entity.TeacherAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for TeacherAvailability entities.
 */
@Repository
public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, Long> {
}
