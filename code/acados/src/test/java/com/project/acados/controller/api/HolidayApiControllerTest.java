package com.project.acados.controller.api;

import com.project.acados.config.SecurityConfig;
import com.project.acados.domain.entity.PublicHoliday;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.HolidayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HolidayApiController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class HolidayApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HolidayService holidayService;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user")
    @DisplayName("GET /api/v1/holidays: authenticated user returns list of holidays -> 200 OK")
    void testGetHolidays_Authenticated_Success() throws Exception {
        PublicHoliday holiday = PublicHoliday.builder()
                .id(1L)
                .date(LocalDate.of(2026, 4, 13))
                .name("วันสงกรานต์")
                .description("Songkran Festival")
                .build();

        when(holidayService.getHolidays()).thenReturn(List.of(holiday));

        mockMvc.perform(get("/api/v1/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("วันสงกรานต์"))
                .andExpect(jsonPath("$[0].description").value("Songkran Festival"))
                .andExpect(jsonPath("$[0].holidayDate").value("2026-04-13"));

        verify(holidayService).getHolidays();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("POST /api/v1/holidays/sync: admin can trigger holiday sync -> 200 OK")
    void testSyncHolidays_Admin_Success() throws Exception {
        PublicHoliday holiday = PublicHoliday.builder()
                .id(1L)
                .date(LocalDate.of(2026, 4, 13))
                .name("วันสงกรานต์")
                .description("Songkran Festival")
                .build();

        when(holidayService.getHolidays()).thenReturn(List.of(holiday));

        mockMvc.perform(post("/api/v1/holidays/sync?year=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("วันสงกรานต์"));

        verify(holidayService).syncHolidays(2026);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    @DisplayName("POST /api/v1/holidays/sync: STUDENT cannot trigger sync -> 403 Forbidden")
    void testSyncHolidays_Student_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/holidays/sync"))
                .andExpect(status().isForbidden());

        verify(holidayService, never()).syncHolidays(anyInt());
        verify(holidayService, never()).syncHolidays();
    }
}
