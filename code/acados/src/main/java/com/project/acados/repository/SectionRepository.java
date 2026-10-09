package com.project.acados.repository;

import com.project.acados.domain.entity.Section;
import com.project.acados.domain.enums.SectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
