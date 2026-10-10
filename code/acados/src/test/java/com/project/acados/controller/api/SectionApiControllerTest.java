package com.project.acados.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.acados.config.SecurityConfig;
import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.dto.request.AssignTeacherRequest;
import com.project.acados.dto.request.SectionRequest;
import com.project.acados.dto.response.SectionResponse;
import com.project.acados.dto.response.TeacherAssignmentOptionResponse;
import com.project.acados.mapper.SectionMapper;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.SectionCancellationService;
import com.project.acados.service.SectionService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SectionApiController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SectionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SectionService sectionService;

    @MockBean
    private SectionCancellationService sectionCancellationService;

    @MockBean
    private SectionMapper sectionMapper;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/sections/{id}/teacher: admin assigns teacher successfully -> 200 OK")
    void testAssignTeacher_Admin_Success() throws Exception {
        Section section = Section.builder().id(100L).sectionNumber(1).capacity(40).build();
        SectionResponse response = new SectionResponse(100L, 1, 40, SectionStatus.ACTIVE, 5L, "CP353002", "Software Design");

        when(sectionService.assignTeacher(100L, 10L)).thenReturn(section);
        when(sectionMapper.toResponse(section)).thenReturn(response);

        AssignTeacherRequest request = new AssignTeacherRequest(10L);

        mockMvc.perform(put("/api/v1/sections/100/teacher")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.sectionNumber").value(1))
                .andExpect(jsonPath("$.courseCode").value("CP353002"));
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    @DisplayName("PUT /api/v1/sections/{id}/teacher: student forbidden -> 403 Forbidden")
    void testAssignTeacher_Student_Forbidden() throws Exception {
        AssignTeacherRequest request = new AssignTeacherRequest(10L);

        mockMvc.perform(put("/api/v1/sections/100/teacher")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/sections/{id}/teachers: returns options with qualification flag -> 200 OK")
    void testGetTeacherOptions_Admin_Success() throws Exception {
        TeacherAssignmentOptionResponse opt1 = new TeacherAssignmentOptionResponse(10L, "Ajarn A", "T001", true);
        TeacherAssignmentOptionResponse opt2 = new TeacherAssignmentOptionResponse(20L, "Ajarn B", "T002", false);

        when(sectionService.getTeacherOptions(100L)).thenReturn(List.of(opt1, opt2));

        mockMvc.perform(get("/api/v1/sections/100/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teacherId").value(10L))
                .andExpect(jsonPath("$[0].qualified").value(true))
                .andExpect(jsonPath("$[1].teacherId").value(20L))
                .andExpect(jsonPath("$[1].qualified").value(false));
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("GET /api/v1/sections: returns list of sections -> 200 OK")
    void testGetSections_Success() throws Exception {
        Section section = Section.builder().id(100L).sectionNumber(1).capacity(40).build();
        SectionResponse response = new SectionResponse(100L, 1, 40, SectionStatus.ACTIVE, 5L, "CP353002", "Software Design");

        when(sectionService.getSections(null)).thenReturn(List.of(section));
        when(sectionMapper.toResponseList(List.of(section))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].courseCode").value("CP353002"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/sections: admin creates section -> 201 Created")
    void testCreateSection_Admin_Success() throws Exception {
        Section section = Section.builder().id(101L).sectionNumber(2).capacity(35).build();
        SectionResponse response = new SectionResponse(101L, 2, 35, SectionStatus.ACTIVE, 5L, "CP353002", "Software Design");

        when(sectionService.createSection(any())).thenReturn(section);
        when(sectionMapper.toResponse(section)).thenReturn(response);

        SectionRequest request = new SectionRequest(5L, 2, 35);

        mockMvc.perform(post("/api/v1/sections")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.sectionNumber").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/sections/{id}/cancel: admin cancels section -> 200 OK")
    void testCancelSection_Admin_Success() throws Exception {
        Section section = Section.builder().id(100L).sectionNumber(1).capacity(40).status(SectionStatus.CANCELLED).build();
        SectionResponse response = new SectionResponse(100L, 1, 40, SectionStatus.CANCELLED, 5L, "CP353002", "Software Design");

        when(sectionService.getSection(100L)).thenReturn(section);
        when(sectionMapper.toResponse(section)).thenReturn(response);

        mockMvc.perform(put("/api/v1/sections/100/cancel").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("GET /api/v1/sections/{id}: returns single section -> 200 OK")
    void testGetSection_Success() throws Exception {
        Section section = Section.builder().id(100L).sectionNumber(1).capacity(40).build();
        SectionResponse response = new SectionResponse(100L, 1, 40, SectionStatus.ACTIVE, 5L, "CP353002", "Software Design");

        when(sectionService.getSection(100L)).thenReturn(section);
        when(sectionMapper.toResponse(section)).thenReturn(response);

        mockMvc.perform(get("/api/v1/sections/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.sectionNumber").value(1))
                .andExpect(jsonPath("$.courseCode").value("CP353002"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/sections/{id}: admin updates section capacity -> 200 OK")
    void testUpdateSection_Admin_Success() throws Exception {
        Section section = Section.builder().id(100L).sectionNumber(1).capacity(50).build();
        SectionResponse response = new SectionResponse(100L, 1, 50, SectionStatus.ACTIVE, 5L, "CP353002", "Software Design");

        when(sectionService.updateSection(eq(100L), any(SectionRequest.class))).thenReturn(section);
        when(sectionMapper.toResponse(section)).thenReturn(response);

        SectionRequest request = new SectionRequest(null, null, 50);

        mockMvc.perform(put("/api/v1/sections/100")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.capacity").value(50));
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    @DisplayName("PUT /api/v1/sections/{id}: student forbidden -> 403 Forbidden")
    void testUpdateSection_Student_Forbidden() throws Exception {
        SectionRequest request = new SectionRequest(null, null, 50);

        mockMvc.perform(put("/api/v1/sections/100")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    @DisplayName("POST /api/v1/sections: student forbidden -> 403 Forbidden")
    void testCreateSection_Student_Forbidden() throws Exception {
        SectionRequest request = new SectionRequest(5L, 2, 35);

        mockMvc.perform(post("/api/v1/sections")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    @DisplayName("PUT /api/v1/sections/{id}/cancel: student forbidden -> 403 Forbidden")
    void testCancelSection_Student_Forbidden() throws Exception {
        mockMvc.perform(put("/api/v1/sections/100/cancel").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    @DisplayName("GET /api/v1/sections/{id}/teachers: student forbidden -> 403 Forbidden")
    void testGetTeacherOptions_Student_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/sections/100/teachers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/sections/{id}/teacher: null teacherId -> 400 Bad Request")
    void testAssignTeacher_ValidationFailure() throws Exception {
        mockMvc.perform(put("/api/v1/sections/100/teacher")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\": null}"))
                .andExpect(status().isBadRequest());
    }
}

