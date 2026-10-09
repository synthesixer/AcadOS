package com.project.acados.repository;

import com.project.acados.domain.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Registrations with student, section and course loaded in the same query, newest first.
     * A null studentId or sectionId means no filter on that column.
     */
    @Query("SELECT r FROM Registration r JOIN FETCH r.student JOIN FETCH r.section JOIN FETCH r.course "
            + "WHERE (:studentId IS NULL OR r.student.id = :studentId) "
            + "AND (:sectionId IS NULL OR r.section.id = :sectionId) "
            + "ORDER BY r.registeredAt DESC")
    List<Registration> findAllWithDetails(@Param("studentId") Long studentId, @Param("sectionId") Long sectionId);
}
