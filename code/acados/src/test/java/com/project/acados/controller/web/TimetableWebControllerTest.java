package com.project.acados.controller.web;

import com.project.acados.security.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.project.acados.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TimetableWebController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("TimetableWebController Unit Tests")
class TimetableWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/schedules returns timetable/grid view")
    void adminSchedulesReturnsGrid() throws Exception {
        mockMvc.perform(get("/admin/schedules"))
                .andExpect(status().isOk())
                .andExpect(view().name("timetable/grid"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/timetable returns timetable/grid view (alias)")
    void adminTimetableReturnsGrid() throws Exception {
        mockMvc.perform(get("/admin/timetable"))
                .andExpect(status().isOk())
                .andExpect(view().name("timetable/grid"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /admin/timetable when role=TEACHER returns 401 Unauthorized")
    void adminTimetable_whenTeacher_returns401() throws Exception {
        mockMvc.perform(get("/admin/timetable"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /admin/schedules when role=TEACHER returns 401 Unauthorized")
    void adminSchedules_whenTeacher_returns401() throws Exception {
        mockMvc.perform(get("/admin/schedules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/schedules when unauthenticated returns 401 Unauthorized")
    void adminSchedules_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/schedules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /teacher/timetable returns timetable/grid view")
    void teacherTimetableReturnsGrid() throws Exception {
        mockMvc.perform(get("/teacher/timetable"))
                .andExpect(status().isOk())
                .andExpect(view().name("timetable/grid"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /student/timetable returns timetable/grid view")
    void studentTimetableReturnsGrid() throws Exception {
        mockMvc.perform(get("/student/timetable"))
                .andExpect(status().isOk())
                .andExpect(view().name("timetable/grid"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /calendar returns timetable/calendar view")
    void calendarReturnsCalendarView() throws Exception {
        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(view().name("timetable/calendar"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/holidays returns timetable/holidays view")
    void holidaysReturnsHolidaysView() throws Exception {
        mockMvc.perform(get("/admin/holidays"))
                .andExpect(status().isOk())
                .andExpect(view().name("timetable/holidays"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /admin/holidays when role=TEACHER returns 401 Unauthorized")
    void holidays_whenTeacher_returns401() throws Exception {
        mockMvc.perform(get("/admin/holidays"))
                .andExpect(status().isUnauthorized());
    }
}

