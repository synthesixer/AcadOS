package com.project.acados.repository;

import com.project.acados.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Notification entity.
 * Reference: class diagram.puml, database.md §2.14
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    List<Notification> findByUserUniversityIdOrderByCreatedAtDesc(String universityId);

    Optional<Notification> findByIdAndUserUniversityId(Long id, String universityId);
}
