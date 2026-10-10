package com.project.acados.controller.api;

import com.project.acados.config.SecurityConfig;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.UserCreateRequest;
import com.project.acados.dto.request.UserUpdateRequest;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserApiController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET /api/v1/users: admin can list users -> 200 OK")
    void testGetUsers_Admin_Success() throws Exception {
        User user = User.builder().id(1L).universityId("T001").email("t001@acados.ac.th").role(UserRole.TEACHER).build();
        when(userService.getUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        when(userService.getFullName(user)).thenReturn("Dr. Teacher");

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].universityId").value("T001"))
                .andExpect(jsonPath("$.content[0].fullName").value("Dr. Teacher"));

        verify(userService).getUsers(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET /api/v1/users/{id}: admin can get user by ID -> 200 OK")
    void testGetUserById_Admin_Success() throws Exception {
        User user = User.builder().id(1L).universityId("T001").email("t001@acados.ac.th").role(UserRole.TEACHER).build();
        when(userService.getUser(1L)).thenReturn(user);
        when(userService.getFullName(user)).thenReturn("Dr. Teacher");

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.universityId").value("T001"));

        verify(userService).getUser(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("POST /api/v1/users: admin creates user -> 201 Created")
    void testCreateUser_Admin_Success() throws Exception {
        UserCreateRequest req = new UserCreateRequest("T002", "Dr. Two", "t002@acados.ac.th", "password123", UserRole.TEACHER);
        User created = User.builder().id(2L).universityId("T002").email("t002@acados.ac.th").role(UserRole.TEACHER).build();
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(created);
        when(userService.getFullName(created)).thenReturn("Dr. Two");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/2"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.universityId").value("T002"));

        verify(userService).createUser(any(UserCreateRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("PUT /api/v1/users/{id}: admin updates user -> 200 OK")
    void testUpdateUser_Admin_Success() throws Exception {
        UserUpdateRequest req = new UserUpdateRequest("Updated Name", "updated@acados.ac.th", null);
        User updated = User.builder().id(1L).universityId("T001").email("updated@acados.ac.th").role(UserRole.TEACHER).build();
        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(updated);
        when(userService.getFullName(updated)).thenReturn("Updated Name");

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@acados.ac.th"));

        verify(userService).updateUser(eq(1L), any(UserUpdateRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("DELETE /api/v1/users/{id}: admin deletes user -> 204 No Content")
    void testDeleteUser_Admin_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    @DisplayName("GET /api/v1/users: Non-admin users cannot manage users -> 403 Forbidden")
    void testGetUsers_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }
}

