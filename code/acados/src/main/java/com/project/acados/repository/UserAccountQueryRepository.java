package com.project.acados.repository;

import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/** Account-only paginated query, kept separate so existing repository files remain untouched. */
public interface UserAccountQueryRepository extends JpaRepository<User, Long> {
    Page<User> findByRoleIn(Collection<UserRole> roles, Pageable pageable);
}
