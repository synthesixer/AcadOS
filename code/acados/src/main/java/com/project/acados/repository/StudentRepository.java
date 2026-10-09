package com.project.acados.repository;

import com.project.acados.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Student entity.
 * Reference: class diagram.puml, database.md §2.2
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByUserUniversityId(String universityId);

    boolean existsByUserId(Long userId);
}
