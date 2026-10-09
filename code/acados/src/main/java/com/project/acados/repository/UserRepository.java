package com.project.acados.repository;

import com.project.acados.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entities.
 * Supports account lookup by university ID and unique-field checks.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUniversityId(String universityId);

    Optional<User> findByEmail(String email);

    boolean existsByUniversityId(String universityId);

    boolean existsByEmail(String email);
}
