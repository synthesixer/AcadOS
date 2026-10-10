package com.project.acados.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.acados.config.SecurityConfig;
import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.TeacherAvailabilityToggleRequest;
import com.project.acados.dto.request.TeacherPreferenceRequest;
import com.project.acados.repository.*;
import com.project.acados.security.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeacherPreferenceApiController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("TeacherPreferenceApiController Unit Tests")
class TeacherPreferenceApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TeacherRepository teacherRepository;

    @MockBean
    private TeacherPreferenceRepository teacherPreferenceRepository;

    @MockBean
    private TeacherQualificationRepository teacherQualificationRepository;

    @MockBean
    private CourseRepository courseRepository;

    @MockBean
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @MockBean
    private TimeSlotRepository timeSlotRepository;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    private User teacherUser;
    private Teacher teacher;
    private Course course;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder().id(1L).universityId("T001").email("t001@kku.ac.th").role(UserRole.TEACHER).build();
        teacher = Teacher.builder().id(10L).fullName("Ajarn Somchai").user(teacherUser).build();
        course = Course.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(3).build();

        when(userRepository.findByUniversityId("T001")).thenReturn(Optional.of(teacherUser));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("GET /api/v1/teacher/preferences: returns teacher preferences")
    void testGetPreferences() throws Exception {
        TeacherPreference pref = TeacherPreference.builder()
                .id(100L)
                .teacher(teacher)
                .course(course)
                .priority(1)
                .build();

        when(teacherPreferenceRepository.findByTeacherIdOrderByPriorityAsc(10L)).thenReturn(List.of(pref));

        mockMvc.perform(get("/api/v1/teacher/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].courseCode").value("CP353002"))
                .andExpect(jsonPath("$[0].priority").value(1));
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("GET /api/v1/teacher/qualifications: returns qualified courses")
    void testGetQualifiedCourses() throws Exception {
        TeacherQualification tq = TeacherQualification.builder().id(1L).teacher(teacher).course(course).build();
        when(teacherQualificationRepository.findByTeacherId(10L)).thenReturn(List.of(tq));

        mockMvc.perform(get("/api/v1/teacher/qualifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5L))
                .andExpect(jsonPath("$[0].courseCode").value("CP353002"));
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("POST /api/v1/teacher/preferences: saves preference when qualified -> 200 OK")
    void testSavePreference_Qualified_Success() throws Exception {
        TeacherPreference pref = TeacherPreference.builder()
                .id(100L)
                .teacher(teacher)
                .course(course)
                .priority(1)
                .build();

        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(teacherPreferenceRepository.findByTeacherId(10L)).thenReturn(List.of());
        when(teacherPreferenceRepository.save(any(TeacherPreference.class))).thenReturn(pref);

        TeacherPreferenceRequest request = new TeacherPreferenceRequest(5L, 1);

        mockMvc.perform(post("/api/v1/teacher/preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("CP353002"))
                .andExpect(jsonPath("$.priority").value(1));
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("POST /api/v1/teacher/preferences: fails when unqualified (BR-06) -> 400 Bad Request")
    void testSavePreference_Unqualified_BadRequest() throws Exception {
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(false);

        TeacherPreferenceRequest request = new TeacherPreferenceRequest(5L, 1);

        mockMvc.perform(post("/api/v1/teacher/preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("DELETE /api/v1/teacher/preferences/{id}: deletes owned preference -> 204 No Content")
    void testDeletePreference_Success() throws Exception {
        TeacherPreference pref = TeacherPreference.builder()
                .id(100L)
                .teacher(teacher)
                .course(course)
                .priority(1)
                .build();

        when(teacherPreferenceRepository.findById(100L)).thenReturn(Optional.of(pref));

        mockMvc.perform(delete("/api/v1/teacher/preferences/100").with(csrf()))
                .andExpect(status().isNoContent());

        verify(teacherPreferenceRepository).delete(pref);
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("DELETE /api/v1/teacher/preferences/{id}: forbidden when deleting other teacher's preference -> 403")
    void testDeletePreference_OtherTeacher_Forbidden() throws Exception {
        Teacher otherTeacher = Teacher.builder().id(99L).build();
        TeacherPreference pref = TeacherPreference.builder()
                .id(100L)
                .teacher(otherTeacher)
                .course(course)
                .priority(1)
                .build();

        when(teacherPreferenceRepository.findById(100L)).thenReturn(Optional.of(pref));

        mockMvc.perform(delete("/api/v1/teacher/preferences/100").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("GET /api/v1/teacher/availabilities: returns slot availability list")
    void testGetAvailabilities() throws Exception {
        TimeSlot slot = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        when(timeSlotRepository.findAll()).thenReturn(List.of(slot));
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(10L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/teacher/availabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timeSlotId").value(1L))
                .andExpect(jsonPath("$[0].isAvailable").value(true));
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("PUT /api/v1/teacher/availabilities: updates slot availability")
    void testUpdateAvailability() throws Exception {
        TimeSlot slot = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        TeacherAvailability avail = TeacherAvailability.builder().id(50L).teacher(teacher).timeSlot(slot).isAvailable(false).build();

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(10L, 1L)).thenReturn(Optional.empty());
        when(teacherAvailabilityRepository.save(any(TeacherAvailability.class))).thenReturn(avail);

        TeacherAvailabilityToggleRequest request = new TeacherAvailabilityToggleRequest(1L, false);

        mockMvc.perform(put("/api/v1/teacher/availabilities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlotId").value(1L))
                .andExpect(jsonPath("$.isAvailable").value(false));
    }

    @Test
    @WithMockUser(username = "S001", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/teacher/preferences: student forbidden -> 403 Forbidden")
    void testPreferences_StudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/preferences"))
                .andExpect(status().isForbidden());
    }
}

