package com.project.acados.repository;

import com.project.acados.domain.entity.Section;
import com.project.acados.domain.enums.SectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Section entity.
 * Reference: class diagram.puml, database.md §2.5
 */
@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByCourseId(Long courseId);

    List<Section> findByStatus(SectionStatus status);

    boolean existsByCourseId(Long courseId);

    boolean existsByCourseIdAndSectionNumber(Long courseId, Integer sectionNumber);

    /**
     * Sections with their course loaded in the same query; courseId null means every course.
     */
    @Query("SELECT s FROM Section s JOIN FETCH s.course c "
            + "WHERE (:courseId IS NULL OR c.id = :courseId) "
            + "ORDER BY c.courseCode, s.sectionNumber")
    List<Section> findAllWithCourse(@Param("courseId") Long courseId);

    @Query("SELECT s FROM Section s JOIN FETCH s.course WHERE s.id = :id")
    Optional<Section> findWithCourseById(@Param("id") Long id);
}
