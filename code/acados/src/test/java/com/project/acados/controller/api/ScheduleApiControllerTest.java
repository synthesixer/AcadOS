package com.project.acados.controller.api;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.SchedulingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleApiController.class)
@org.springframework.context.annotation.Import(com.project.acados.config.SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("ScheduleApiController Unit Tests")
class ScheduleApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchedulingService schedulingService;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/schedules/generate: ADMIN can trigger draft generation")
    void adminCanGenerateSchedule() throws Exception {
        Course course = Course.builder().courseCode("CP353002").title("Architecture").build();
        Section section = Section.builder().id(10L).sectionNumber(1).course(course).build();
        Teacher teacher = Teacher.builder().id(20L).fullName("Dr. Somchai").build();
        Room room = Room.builder().id(30L).building("SC-01").roomNumber("401").build();
        TimeSlot slot = TimeSlot.builder().id(100L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();

        Schedule schedule = Schedule.builder()
                .id(1L)
                .section(section)
                .teacher(teacher)
                .room(room)
                .timeSlot(slot)
                .status(ScheduleStatus.DRAFT)
                .build();

        doNothing().when(schedulingService).generateSchedule();
        when(schedulingService.getDraftSchedules()).thenReturn(List.of(schedule));

        mockMvc.perform(post("/api/v1/schedules/generate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].courseCode").value("CP353002"))
                .andExpect(jsonPath("$[0].teacherName").value("Dr. Somchai"));

        verify(schedulingService).generateSchedule();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/v1/schedules: should return published schedules")
    void getSchedulesShouldReturnPublished() throws Exception {
        Course course = Course.builder().courseCode("CP353002").title("Architecture").build();
        Section section = Section.builder().id(10L).sectionNumber(1).course(course).build();
        Schedule schedule = Schedule.builder()
                .id(1L)
                .section(section)
                .status(ScheduleStatus.PUBLISHED)
                .build();

        when(schedulingService.getPublishedSchedules()).thenReturn(List.of(schedule));

        mockMvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/schedules/publish: ADMIN can publish drafts")
    void adminCanPublishSchedules() throws Exception {
        doNothing().when(schedulingService).publishSchedule();

        mockMvc.perform(put("/api/v1/schedules/publish").with(csrf()))
                .andExpect(status().isOk());

        verify(schedulingService).publishSchedule();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/schedules/draft: ADMIN can discard drafts")
    void adminCanDiscardDrafts() throws Exception {
        doNothing().when(schedulingService).discardDraft();

        mockMvc.perform(delete("/api/v1/schedules/draft").with(csrf()))
                .andExpect(status().isNoContent());

        verify(schedulingService).discardDraft();
    }
}

