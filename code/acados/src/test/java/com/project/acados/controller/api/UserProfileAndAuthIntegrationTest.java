package com.project.acados.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.ChangePasswordRequest;
import com.project.acados.dto.request.LoginRequest;
import com.project.acados.dto.request.TeacherPreferenceRequest;
import com.project.acados.repository.UserRepository;
import com.project.acados.repository.TeacherRepository;
import com.project.acados.repository.StudentRepository;
import com.project.acados.repository.CourseRepository;
import com.project.acados.repository.TeacherQualificationRepository;
import com.project.acados.repository.TeacherPreferenceRepository;
import com.project.acados.repository.TeacherAvailabilityRepository;
import com.project.acados.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("User Profile, Auth with Email, and Teacher Preference Integration Tests")
class UserProfileAndAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherQualificationRepository qualificationRepository;

    @Autowired
    private TeacherPreferenceRepository preferenceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User teacherUser;
    private Teacher teacher;
    private Course course;

    @BeforeEach
    void setUp() {
        teacherUser = userRepository.save(User.builder()
                .universityId("T-TEST01")
                .email("teacher.test@acados.kku.ac.th")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.TEACHER)
                .build());

        teacher = teacherRepository.save(Teacher.builder()
                .user(teacherUser)
                .fullName("อาจารย์ ทดสอบ สอนดี")
                .build());

        course = courseRepository.save(Course.builder()
                .courseCode("CP999001")
                .title("Advanced Software Testing")
                .weeklyHours(3)
                .build());

        qualificationRepository.save(TeacherQualification.builder()
                .teacher(teacher)
                .course(course)
                .build());
    }

    @Test
    @DisplayName("Should login successfully using email with same password")
    void shouldLoginWithEmailSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("teacher.test@acados.kku.ac.th", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    @DisplayName("Should login successfully using universityId")
    void shouldLoginWithUniversityIdSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("T-TEST01", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    @WithMockUser(username = "T-TEST01", roles = {"TEACHER"})
    @DisplayName("Should get user profile info via /api/v1/auth/me")
    void shouldGetMeProfile() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universityId").value("T-TEST01"))
                .andExpect(jsonPath("$.email").value("teacher.test@acados.kku.ac.th"))
                .andExpect(jsonPath("$.fullName").value("อาจารย์ ทดสอบ สอนดี"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.teacherId").value(teacher.getId()));
    }

    @Test
    @WithMockUser(username = "T-TEST01", roles = {"TEACHER"})
    @DisplayName("Should change password successfully via /api/v1/auth/change-password")
    void shouldChangePasswordSuccessfully() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "newSecret999");

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User updated = userRepository.findByUniversityId("T-TEST01").orElseThrow();
        assertTrue(passwordEncoder.matches("newSecret999", updated.getPasswordHash()));
    }

    @Test
    @WithMockUser(username = "T-TEST01", roles = {"TEACHER"})
    @DisplayName("Should fail password change if current password is wrong")
    void shouldFailChangePasswordWithWrongCurrentPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongOldPassword", "newSecret999");

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "T-TEST01", roles = {"TEACHER"})
    @DisplayName("Teacher can save and view course preferences via /api/v1/teacher/preferences")
    void teacherCanManageCoursePreferences() throws Exception {
        // 1. Get qualified courses
        mockMvc.perform(get("/api/v1/teacher/qualifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("CP999001"));

        // 2. Save preference (priority 1 = highest)
        TeacherPreferenceRequest saveReq = new TeacherPreferenceRequest(course.getId(), 1);
        mockMvc.perform(post("/api/v1/teacher/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("CP999001"))
                .andExpect(jsonPath("$.priority").value(1));

        // 3. Get preferences
        mockMvc.perform(get("/api/v1/teacher/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("CP999001"))
                .andExpect(jsonPath("$[0].priority").value(1));
    }

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    @WithMockUser(username = "T-TEST01", roles = {"TEACHER"})
    @DisplayName("Teacher can manage availability (unavailable slots) via /api/v1/teacher/availabilities")
    void teacherCanManageAvailabilities() throws Exception {
        TimeSlot slot = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(java.time.DayOfWeek.MONDAY)
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(12, 0))
                .build());

        // 1. Get availabilities (defaults to isAvailable = true)
        mockMvc.perform(get("/api/v1/teacher/availabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.timeSlotId == " + slot.getId() + ")].isAvailable").value(true));

        // 2. Mark slot as unavailable (isAvailable = false)
        com.project.acados.dto.request.TeacherAvailabilityToggleRequest toggleReq =
                new com.project.acados.dto.request.TeacherAvailabilityToggleRequest(slot.getId(), false);

        mockMvc.perform(put("/api/v1/teacher/availabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toggleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlotId").value(slot.getId()))
                .andExpect(jsonPath("$.isAvailable").value(false));

        // 3. Verify slot is now unavailable
        mockMvc.perform(get("/api/v1/teacher/availabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.timeSlotId == " + slot.getId() + ")].isAvailable").value(false));
    }
}

