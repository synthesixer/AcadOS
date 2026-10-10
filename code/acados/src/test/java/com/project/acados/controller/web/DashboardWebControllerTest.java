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

import com.project.acados.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DashboardWebController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("DashboardWebController Unit Tests - Role-Based Web Security & Views")
class DashboardWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    // ==========================================
    // 1. Admin Routes - ADMIN Access (200 OK)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/dashboard: ADMIN -> 200 OK, view admin/dashboard")
    void adminDashboard_whenAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/swaps: ADMIN -> 200 OK, view admin/swaps")
    void adminSwaps_whenAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/admin/swaps"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/swaps"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/courses: ADMIN -> 200 OK, view admin/courses")
    void adminCourses_whenAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/admin/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/courses"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/rooms: ADMIN -> 200 OK, view admin/rooms")
    void adminRooms_whenAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/admin/rooms"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/rooms"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/sections: ADMIN -> 200 OK, view admin/sections")
    void adminSections_whenAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/admin/sections"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/sections"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/users: ADMIN -> 200 OK, view admin/users")
    void adminUsers_whenAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/registrations: ADMIN -> 200 OK, view admin/registrations")
    void adminRegistrations_whenAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/admin/registrations"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/registrations"));
    }

    // =========================================================================
    // 2. Admin Routes - Non-Admin Access -> 401 Unauthorized (Activity Diagram)
    // =========================================================================

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /admin/dashboard: TEACHER -> 403 Forbidden")
    void adminDashboard_whenTeacher_returns403() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /admin/swaps: TEACHER -> 403 Forbidden")
    void adminSwaps_whenTeacher_returns403() throws Exception {
        mockMvc.perform(get("/admin/swaps"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/dashboard: unauthenticated -> 401 Unauthorized")
    void adminDashboard_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /admin/courses: STUDENT -> 403 Forbidden")
    void adminCourses_whenStudent_returns403() throws Exception {
        mockMvc.perform(get("/admin/courses"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. Teacher Routes
    // ==========================================

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /teacher/dashboard: TEACHER -> 200 OK, view teacher/dashboard")
    void teacherDashboard_whenTeacher_returnsOk() throws Exception {
        mockMvc.perform(get("/teacher/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/dashboard"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /teacher/swaps: TEACHER -> 200 OK, view teacher/swaps")
    void teacherSwaps_whenTeacher_returnsOk() throws Exception {
        mockMvc.perform(get("/teacher/swaps"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/swaps"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /teacher/dashboard: STUDENT -> 403 Forbidden")
    void teacherDashboard_whenStudent_returns403() throws Exception {
        mockMvc.perform(get("/teacher/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /teacher/dashboard: unauthenticated -> 401 Unauthorized")
    void teacherDashboard_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/teacher/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 4. Student Routes
    // ==========================================

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /student/dashboard: STUDENT -> 200 OK, view student/dashboard")
    void studentDashboard_whenStudent_returnsOk() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/dashboard"));
    }

    @Test
    @DisplayName("GET /student/dashboard: unauthenticated -> 401 Unauthorized")
    void studentDashboard_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /student/courses: STUDENT -> 200 OK, view student/courses")
    void studentCourses_whenStudent_returnsOk() throws Exception {
        mockMvc.perform(get("/student/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/courses"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /student/registrations: STUDENT -> 200 OK, view student/registrations")
    void studentRegistrations_whenStudent_returnsOk() throws Exception {
        mockMvc.perform(get("/student/registrations"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/registrations"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /student/courses: TEACHER -> 403 Forbidden")
    void studentCourses_whenTeacher_returns403() throws Exception {
        mockMvc.perform(get("/student/courses"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 5. Notifications Route (Authenticated)
    // ==========================================

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /notifications: authenticated -> 200 OK, view notifications")
    void notifications_whenAuthenticated_returnsOk() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"));
    }
}

