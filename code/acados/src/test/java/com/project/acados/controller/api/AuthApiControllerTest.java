package com.project.acados.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.acados.config.SecurityConfig;
import com.project.acados.domain.entity.Student;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.ChangePasswordRequest;
import com.project.acados.dto.request.LoginRequest;
import com.project.acados.repository.StudentRepository;
import com.project.acados.repository.TeacherRepository;
import com.project.acados.repository.UserRepository;
import com.project.acados.security.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthApiController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthApiController WebMvc Slice Tests")
class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TeacherRepository teacherRepository;

    @MockBean
    private StudentRepository studentRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/v1/auth/login: login success returns token and cookie")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest("admin", "password123");

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "admin",
                        "password123",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(any(UserDetails.class))).thenReturn("jwt-token-xyz");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-xyz"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(cookie().exists("acados_token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: invalid credentials returns 401 Unauthorized")
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrong");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("GET /api/v1/auth/me: returns teacher profile info -> 200 OK")
    void testGetCurrentUser_Teacher_Success() throws Exception {
        User user = User.builder().id(10L).universityId("T001").email("t001@kku.ac.th").role(UserRole.TEACHER).build();
        Teacher teacher = Teacher.builder().id(100L).user(user).fullName("Ajarn Somchai").build();

        when(userRepository.findByUniversityId("T001")).thenReturn(Optional.of(user));
        when(teacherRepository.findByUserId(10L)).thenReturn(Optional.of(teacher));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universityId").value("T001"))
                .andExpect(jsonPath("$.fullName").value("Ajarn Somchai"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.teacherId").value(100L));
    }

    @Test
    @WithMockUser(username = "S001", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/auth/me: returns student profile info -> 200 OK")
    void testGetCurrentUser_Student_Success() throws Exception {
        User user = User.builder().id(20L).universityId("S001").email("s001@kku.ac.th").role(UserRole.STUDENT).build();
        Student student = Student.builder().id(200L).user(user).fullName("Student Somkid").build();

        when(userRepository.findByUniversityId("S001")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(20L)).thenReturn(Optional.of(student));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universityId").value("S001"))
                .andExpect(jsonPath("$.fullName").value("Student Somkid"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.studentId").value(200L));
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("PUT /api/v1/auth/change-password: success when old password correct -> 200 OK")
    void testChangePassword_Success() throws Exception {
        User user = User.builder().id(10L).universityId("T001").passwordHash("oldHashed").role(UserRole.TEACHER).build();

        when(userRepository.findByUniversityId("T001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "oldHashed")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHashed");

        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userRepository).save(user);
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("PUT /api/v1/auth/change-password: fails when old password wrong -> 400 Bad Request")
    void testChangePassword_WrongOldPassword() throws Exception {
        User user = User.builder().id(10L).universityId("T001").passwordHash("oldHashed").role(UserRole.TEACHER).build();

        when(userRepository.findByUniversityId("T001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "oldHashed")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass");

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }
}

