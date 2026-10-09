package com.project.acados.repository;

import com.project.acados.domain.entity.TeacherPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for TeacherPreference entities.
 * Reference: database.md §2.11, class diagram.puml
 */
@Repository
public interface TeacherPreferenceRepository extends JpaRepository<TeacherPreference, Long> {

    List<TeacherPreference> findByTeacherId(Long teacherId);

    List<TeacherPreference> findByTeacherIdOrderByPriorityAsc(Long teacherId);

    boolean existsByTeacherIdAndCourseId(Long teacherId, Long courseId);
}
