package com.project.acados.controller.api;

import com.project.acados.config.SecurityConfig;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.TeacherSwapRequest;
import com.project.acados.domain.enums.SwapStatus;
import com.project.acados.dto.request.SwapCreateRequest;
import com.project.acados.dto.request.SwapResponseRequest;
import com.project.acados.dto.response.SwapInboxResponse;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.TeacherSwapQueryService;
import com.project.acados.service.TeacherSwapService;
import com.project.acados.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeacherSwapApiController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TeacherSwapApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeacherSwapService teacherSwapService;

    @MockBean
    private TeacherSwapQueryService queryService;

    @MockBean
    private UserService userService;

    @MockBean
    private ScheduleRepository scheduleRepository;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "T001", roles = "TEACHER")
    @DisplayName("GET /api/v1/teacher-swaps: should return SwapInboxResponse for authenticated teacher")
    void testGetMyRequests_Success() throws Exception {
        when(userService.getTeacherIdByUniversityId("T001")).thenReturn(1L);
        SwapInboxResponse inbox = new SwapInboxResponse(List.of(), List.of());
        when(queryService.getRequestsForTeacher(1L)).thenReturn(inbox);

        mockMvc.perform(get("/api/v1/teacher-swaps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").isArray())
                .andExpect(jsonPath("$.received").isArray());

        verify(queryService).getRequestsForTeacher(1L);
    }

    @Test
    @WithMockUser(username = "T001", roles = "TEACHER")
    @DisplayName("POST /api/v1/teacher-swaps: should create swap request and return 201 Created")
    void testCreateSwap_Success() throws Exception {
        SwapCreateRequest req = new SwapCreateRequest(101L, 2L, 202L);

        Teacher teacherA = Teacher.builder().id(1L).fullName("Dr. A").build();
        Teacher teacherB = Teacher.builder().id(2L).fullName("Dr. B").build();
        TeacherSwapRequest saved = TeacherSwapRequest.builder()
                .id(55L)
                .requestingTeacher(teacherA)
                .targetTeacher(teacherB)
                .status(SwapStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getTeacherIdByUniversityId("T001")).thenReturn(1L);
        when(teacherSwapService.createSwapRequest(1L, 101L, 2L, 202L)).thenReturn(saved);

        mockMvc.perform(post("/api/v1/teacher-swaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/teacher-swaps/55"))
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(teacherSwapService).createSwapRequest(1L, 101L, 2L, 202L);
    }

    @Test
    @WithMockUser(username = "T002", roles = "TEACHER")
    @DisplayName("PUT /api/v1/teacher-swaps/{id}/respond: should respond to swap and return 200 OK")
    void testRespondSwap_Success() throws Exception {
        SwapResponseRequest req = new SwapResponseRequest(true);
        when(userService.getTeacherIdByUniversityId("T002")).thenReturn(2L);

        mockMvc.perform(put("/api/v1/teacher-swaps/55/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(teacherSwapService).respondSwap(55L, 2L, true);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("PUT /api/v1/teacher-swaps/{id}/approve: admin approves swap -> 200 OK")
    void testApproveSwap_Success() throws Exception {
        mockMvc.perform(put("/api/v1/teacher-swaps/55/approve"))
                .andExpect(status().isOk());

        verify(teacherSwapService).approveSwap(55L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("PUT /api/v1/teacher-swaps/{id}/reject: admin rejects swap -> 200 OK")
    void testRejectSwap_Success() throws Exception {
        mockMvc.perform(put("/api/v1/teacher-swaps/55/reject"))
                .andExpect(status().isOk());

        verify(teacherSwapService).rejectSwap(55L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET /api/v1/teacher-swaps/all: admin retrieves all swap requests -> 200 OK")
    void testGetAllRequests_Admin_Success() throws Exception {
        when(queryService.getAllRequests()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/teacher-swaps/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(queryService).getAllRequests();
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    @DisplayName("GET /api/v1/teacher-swaps/all: STUDENT cannot access admin endpoints -> 403 Forbidden")
    void testGetAllRequests_Student_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/teacher-swaps/all"))
                .andExpect(status().isForbidden());
    }
}

