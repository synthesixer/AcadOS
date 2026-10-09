package com.project.acados.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.acados.security.TokenProvider;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Renders the welcome page through the master layout and checks the role-based sidebar.
 */
@WebMvcTest
@ActiveProfiles("test")
class LayoutRenderingTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Layout: admin sees admin menu only, with shared assets and page content")
    void layout_whenAdmin_showsAdminMenu() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/app.css")))
                .andExpect(content().string(containsString("/js/api.js")))
                .andExpect(content().string(containsString("/js/ui.js")))
                .andExpect(content().string(containsString("<title>AcadOS</title>")))
                .andExpect(content().string(containsString("Automated Proctor Scheduling")))
                .andExpect(content().string(containsString("/admin/dashboard")))
                .andExpect(content().string(containsString("/notifications")))
                .andExpect(content().string(containsString("logout-button")))
                .andExpect(content().string(not(containsString("/teacher/dashboard"))))
                .andExpect(content().string(not(containsString("/student/dashboard"))));
    }

    @Test
    @WithMockUser(username = "T001", roles = "TEACHER")
    @DisplayName("Layout: teacher sees teacher menu only")
    void layout_whenTeacher_showsTeacherMenu() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/teacher/swaps")))
                .andExpect(content().string(not(containsString("/admin/dashboard"))))
                .andExpect(content().string(not(containsString("/student/dashboard"))));
    }

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    @DisplayName("Layout: student sees student menu only and their login id in the topbar")
    void layout_whenStudent_showsStudentMenu() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/student/courses")))
                .andExpect(content().string(containsString("S001")))
                .andExpect(content().string(not(containsString("/admin/dashboard"))))
                .andExpect(content().string(not(containsString("/teacher/dashboard"))));
    }
}
