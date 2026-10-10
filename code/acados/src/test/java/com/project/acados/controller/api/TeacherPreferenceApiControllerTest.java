package com.project.acados.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.acados.config.SecurityConfig;
import com.project.acados.dto.request.TeacherAvailabilityToggleRequest;
import com.project.acados.dto.request.TeacherPreferenceRequest;
import com.project.acados.dto.response.CourseResponse;
import com.project.acados.dto.response.TeacherAvailabilityResponse;
import com.project.acados.dto.response.TeacherPreferenceResponse;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.TeacherPreferenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private TeacherPreferenceService teacherPreferenceService;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("GET /api/v1/teacher/preferences: returns teacher preferences")
    void testGetPreferences() throws Exception {
        TeacherPreferenceResponse resp = new TeacherPreferenceResponse(100L, 5L, "CP353002", "Software Design", 1);
        when(teacherPreferenceService.getPreferences("T001")).thenReturn(List.of(resp));

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
        CourseResponse resp = CourseResponse.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(3).build();
        when(teacherPreferenceService.getQualifiedCourses("T001")).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/teacher/qualifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5L))
                .andExpect(jsonPath("$[0].courseCode").value("CP353002"));
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("POST /api/v1/teacher/preferences: saves preference when qualified -> 200 OK")
    void testSavePreference_Qualified_Success() throws Exception {
        TeacherPreferenceResponse resp = new TeacherPreferenceResponse(100L, 5L, "CP353002", "Software Design", 1);
        when(teacherPreferenceService.savePreference(any(TeacherPreferenceRequest.class), eq("T001"))).thenReturn(resp);

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
        when(teacherPreferenceService.savePreference(any(TeacherPreferenceRequest.class), eq("T001")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ท่านยังไม่มีคุณสมบัติในการสอนรายวิชานี้ (BR-06)"));

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
        doNothing().when(teacherPreferenceService).deletePreference(100L, "T001");

        mockMvc.perform(delete("/api/v1/teacher/preferences/100").with(csrf()))
                .andExpect(status().isNoContent());

        verify(teacherPreferenceService).deletePreference(100L, "T001");
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("DELETE /api/v1/teacher/preferences/{id}: forbidden when deleting other teacher's preference -> 403")
    void testDeletePreference_OtherTeacher_Forbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "ไม่สามารถลบข้อมูลของอาจารย์ท่านอื่นได้"))
                .when(teacherPreferenceService).deletePreference(100L, "T001");

        mockMvc.perform(delete("/api/v1/teacher/preferences/100").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("GET /api/v1/teacher/availabilities: returns slot availability list")
    void testGetAvailabilities() throws Exception {
        TeacherAvailabilityResponse resp = new TeacherAvailabilityResponse(null, 1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), true);
        when(teacherPreferenceService.getAvailabilities("T001")).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/teacher/availabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timeSlotId").value(1L))
                .andExpect(jsonPath("$[0].isAvailable").value(true));
    }

    @Test
    @WithMockUser(username = "T001", roles = {"TEACHER"})
    @DisplayName("PUT /api/v1/teacher/availabilities: updates slot availability")
    void testUpdateAvailability() throws Exception {
        TeacherAvailabilityResponse resp = new TeacherAvailabilityResponse(50L, 1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), false);
        when(teacherPreferenceService.updateAvailability(any(TeacherAvailabilityToggleRequest.class), eq("T001"))).thenReturn(resp);

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
