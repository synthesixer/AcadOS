package com.project.acados.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.acados.config.SecurityConfig;
import com.project.acados.domain.entity.AcademicEvent;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.dto.request.AcademicEventRequest;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.AcademicEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AcademicEventApiController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("AcademicEventApiController Unit Tests")
class AcademicEventApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AcademicEventService academicEventService;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/v1/academic-events: should return all events")
    void getEventsShouldReturnAllEvents() throws Exception {
        AcademicEvent event = AcademicEvent.builder()
                .id(1L)
                .eventName("Semester 1/2026 Registration")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 15))
                .build();

        when(academicEventService.getEvents()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/academic-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].eventName").value("Semester 1/2026 Registration"))
                .andExpect(jsonPath("$[0].eventType").value("REGISTRATION_PERIOD"))
                .andExpect(jsonPath("$[0].startDate").value("2026-06-01"))
                .andExpect(jsonPath("$[0].endDate").value("2026-06-15"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /api/v1/academic-events?type=MIDTERM_EXAM: should return filtered events")
    void getEventsWithTypeFilterShouldReturnFilteredEvents() throws Exception {
        AcademicEvent event = AcademicEvent.builder()
                .id(2L)
                .eventName("Midterm Exam Period")
                .eventType(AcademicEventType.MIDTERM_EXAM)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 7))
                .build();

        when(academicEventService.getEventsByType(AcademicEventType.MIDTERM_EXAM)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/academic-events").param("type", "MIDTERM_EXAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].eventType").value("MIDTERM_EXAM"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/v1/academic-events/{id}: should return single event")
    void getEventByIdShouldReturnEvent() throws Exception {
        AcademicEvent event = AcademicEvent.builder()
                .id(1L)
                .eventName("Semester Start")
                .eventType(AcademicEventType.SEMESTER_START)
                .startDate(LocalDate.of(2026, 6, 16))
                .endDate(LocalDate.of(2026, 6, 16))
                .build();

        when(academicEventService.getEvent(1L)).thenReturn(event);

        mockMvc.perform(get("/api/v1/academic-events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.eventName").value("Semester Start"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/academic-events: ADMIN can create event")
    void adminCanCreateEvent() throws Exception {
        AcademicEventRequest request = AcademicEventRequest.builder()
                .eventName("Final Exam 1/2026")
                .eventType(AcademicEventType.FINAL_EXAM)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 14))
                .build();

        AcademicEvent created = AcademicEvent.builder()
                .id(3L)
                .eventName("Final Exam 1/2026")
                .eventType(AcademicEventType.FINAL_EXAM)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 14))
                .build();

        when(academicEventService.createEvent(any(AcademicEventRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/academic-events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.eventName").value("Final Exam 1/2026"))
                .andExpect(jsonPath("$.eventType").value("FINAL_EXAM"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/academic-events: non-ADMIN gets 403 Forbidden")
    void studentCannotCreateEvent() throws Exception {
        AcademicEventRequest request = AcademicEventRequest.builder()
                .eventName("Student Event")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 15))
                .build();

        mockMvc.perform(post("/api/v1/academic-events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(academicEventService, never()).createEvent(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/academic-events: blank eventName returns 400 Bad Request")
    void createEventValidationFailsWhenBlankName() throws Exception {
        AcademicEventRequest request = AcademicEventRequest.builder()
                .eventName("")
                .eventType(AcademicEventType.FINAL_EXAM)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 14))
                .build();

        mockMvc.perform(post("/api/v1/academic-events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/academic-events/{id}: ADMIN can update event")
    void adminCanUpdateEvent() throws Exception {
        AcademicEventRequest request = AcademicEventRequest.builder()
                .eventName("Updated Registration")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 20))
                .build();

        AcademicEvent updated = AcademicEvent.builder()
                .id(1L)
                .eventName("Updated Registration")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 20))
                .build();

        when(academicEventService.updateEvent(eq(1L), any(AcademicEventRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/academic-events/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.eventName").value("Updated Registration"))
                .andExpect(jsonPath("$.endDate").value("2026-06-20"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/academic-events/{id}: ADMIN can delete event")
    void adminCanDeleteEvent() throws Exception {
        doNothing().when(academicEventService).deleteEvent(1L);

        mockMvc.perform(delete("/api/v1/academic-events/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(academicEventService, times(1)).deleteEvent(1L);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("DELETE /api/v1/academic-events/{id}: TEACHER cannot delete event")
    void teacherCannotDeleteEvent() throws Exception {
        mockMvc.perform(delete("/api/v1/academic-events/1").with(csrf()))
                .andExpect(status().isForbidden());

        verify(academicEventService, never()).deleteEvent(any());
    }
}

