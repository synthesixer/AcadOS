package com.project.acados.repository;

import com.project.acados.domain.entity.TeacherPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for TeacherPreference entities.
 */
@Repository
public interface TeacherPreferenceRepository extends JpaRepository<TeacherPreference, Long> {
}
