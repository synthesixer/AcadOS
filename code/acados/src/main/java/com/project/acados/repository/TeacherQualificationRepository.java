package com.project.acados.repository;

import com.project.acados.domain.entity.TeacherQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for TeacherQualification entities.
 */
@Repository
public interface TeacherQualificationRepository extends JpaRepository<TeacherQualification, Long> {

    boolean existsByTeacherIdAndCourseId(Long teacherId, Long courseId);

    List<TeacherQualification> findByCourseId(Long courseId);

    List<TeacherQualification> findByTeacherId(Long teacherId);
}
