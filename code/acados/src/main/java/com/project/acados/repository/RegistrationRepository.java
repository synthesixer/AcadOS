package com.project.acados.repository;

import com.project.acados.domain.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Registration entity.
 * Used for registration rules BR-03, BR-04, BR-05 and section cancellation.
 * Reference: class diagram.puml, database.md §2.9
 */
@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    long countBySectionId(Long sectionId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentId(Long studentId);

    List<Registration> findByStudentId(Long studentId);

    List<Registration> findBySectionId(Long sectionId);

    Optional<Registration> findByIdAndStudentId(Long id, Long studentId);

    void deleteBySectionId(Long sectionId);
}
