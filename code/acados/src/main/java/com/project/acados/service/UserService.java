package com.project.acados.service;

import com.project.acados.domain.entity.User;
import com.project.acados.dto.request.UserCreateRequest;
import com.project.acados.dto.request.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<User> getUsers(Pageable pageable);
    User getUser(Long id);
    User createUser(UserCreateRequest request);
    User updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);

    /** Resolves the authenticated University ID to the teacher snapshot ID used by swap services. */
    Long getTeacherIdByUniversityId(String universityId);

    String getFullName(User user);
}
