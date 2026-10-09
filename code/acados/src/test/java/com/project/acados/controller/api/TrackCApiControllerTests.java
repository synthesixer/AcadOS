package com.project.acados.controller.api;

import com.project.acados.config.SecurityConfig;
import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.Registration;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Student;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.dto.request.SectionRequest;
import com.project.acados.mapper.NotificationMapperImpl;
import com.project.acados.mapper.RegistrationMapperImpl;
import com.project.acados.mapper.SectionMapperImpl;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.NotificationService;
import com.project.acados.service.RegistrationService;
import com.project.acados.service.SectionCancellationService;
import com.project.acados.service.SectionService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests of the Track C REST controllers with the real security rules:
 * status codes, role checks, request validation and the JSON shape of the response DTOs.
 */
@WebMvcTest({RegistrationApiController.class, SectionApiController.class, NotificationApiController.class})
@Import({SecurityConfig.class, RegistrationMapperImpl.class, SectionMapperImpl.class, NotificationMapperImpl.class})
@ActiveProfiles("test")
class TrackCApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private SectionService sectionService;

    @MockBean
    private SectionCancellationService sectionCancellationService;

    @MockBean
    private NotificationService notificationService;

    // Dependencies of JwtAuthenticationFilter, which @WebMvcTest loads as a Filter bean.
    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    private Section section;
    private Registration registration;

    @BeforeEach
    void setUp() {
        Course course = Course.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(3).build();
        section = Section.builder().id(100L).course(course).sectionNumber(1).capacity(40).build();
        Student student = Student.builder().id(10L).fullName("Student One").build();
        registration = Registration.builder().id(900L).student(student).section(section).course(course)
                .registeredAt(LocalDateTime.of(2026, 10, 10, 9, 0)).build();
    }

    // ----- Registrations -----

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void register_asStudent_returns201WithLocationAndDto() throws Exception {
        when(registrationService.register("S001", 100L)).thenReturn(registration);

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionId\": 100}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/registrations/900"))
                .andExpect(jsonPath("$.id").value(900))
                .andExpect(jsonPath("$.studentId").value(10))
                .andExpect(jsonPath("$.studentName").value("Student One"))
                .andExpect(jsonPath("$.sectionId").value(100))
                .andExpect(jsonPath("$.sectionNumber").value(1))
                .andExpect(jsonPath("$.courseId").value(5))
                .andExpect(jsonPath("$.courseCode").value("CP353002"))
                .andExpect(jsonPath("$.courseTitle").value("Software Design"));
    }

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void register_withoutSectionId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrationService);
    }

    @Test
    @WithMockUser(username = "T001", roles = "TEACHER")
    void register_asTeacher_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionId\": 100}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(registrationService);
    }

    @Test
    void register_withoutLogin_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionId\": 100}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void withdraw_asStudent_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/registrations/900"))
                .andExpect(status().isNoContent());

        verify(registrationService).withdraw("S001", 900L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getRegistrations_asAdmin_passesSectionFilter() throws Exception {
        when(registrationService.getRegistrations("admin", 100L)).thenReturn(List.of(registration));

        mockMvc.perform(get("/api/v1/registrations").param("sectionId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(900));
    }

    @Test
    @WithMockUser(username = "T001", roles = "TEACHER")
    void getRegistrations_asTeacher_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/registrations"))
                .andExpect(status().isForbidden());
    }

    // ----- Sections -----

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void getSections_asStudent_returnsSectionDtos() throws Exception {
        when(sectionService.getSections(5L)).thenReturn(List.of(section));

        mockMvc.perform(get("/api/v1/sections").param("courseId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].sectionNumber").value(1))
                .andExpect(jsonPath("$[0].capacity").value(40))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].courseId").value(5))
                .andExpect(jsonPath("$[0].courseCode").value("CP353002"))
                .andExpect(jsonPath("$[0].courseTitle").value("Software Design"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createSection_asAdmin_returns201() throws Exception {
        when(sectionService.createSection(new SectionRequest(5L, 1, 40))).thenReturn(section);

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\": 5, \"sectionNumber\": 1, \"capacity\": 40}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sections/100"))
                .andExpect(jsonPath("$.courseCode").value("CP353002"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createSection_withoutCourseId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionNumber\": 1, \"capacity\": 40}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sectionService);
    }

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void createSection_asStudent_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\": 5, \"sectionNumber\": 1, \"capacity\": 40}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateSection_withCapacityOnly_returns200() throws Exception {
        when(sectionService.updateSection(eq(100L), any(SectionRequest.class))).thenReturn(section);

        mockMvc.perform(put("/api/v1/sections/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\": 40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(40));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateSection_withZeroCapacity_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/sections/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cancelSection_asAdmin_cancelsAndReturnsSection() throws Exception {
        when(sectionService.getSection(100L)).thenReturn(section);

        mockMvc.perform(put("/api/v1/sections/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));

        verify(sectionCancellationService).cancelSection(100L);
    }

    @Test
    @WithMockUser(username = "T001", roles = "TEACHER")
    void cancelSection_asTeacher_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/sections/100/cancel"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sectionCancellationService);
    }

    // ----- Notifications -----

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void getNotifications_returnsOwnNotificationDtos() throws Exception {
        Notification notification = Notification.builder()
                .id(10L)
                .type(NotificationType.REGISTRATION_SUCCESS)
                .title("ลงทะเบียนสำเร็จ")
                .message("คุณลงทะเบียน CP353002 Section 1 เรียบร้อยแล้ว")
                .createdAt(LocalDateTime.of(2026, 10, 10, 9, 0))
                .build();
        when(notificationService.getNotifications("S001")).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("REGISTRATION_SUCCESS"))
                .andExpect(jsonPath("$[0].title").value("ลงทะเบียนสำเร็จ"))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void markAsRead_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/10/read"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead("S001", 10L);
    }

    @Test
    void getNotifications_withoutLogin_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
