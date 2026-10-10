package com.project.acados.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.acados.domain.entity.Course;
import com.project.acados.dto.request.CourseRequest;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseApiController.class)
@org.springframework.context.annotation.Import(com.project.acados.config.SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("CourseApiController Unit Tests")
class CourseApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/v1/courses: should return paginated courses")
    void getCoursesShouldReturnPagedCourses() throws Exception {
        Course course = Course.builder()
                .id(1L)
                .courseCode("CP353002")
                .title("Software Design")
                .weeklyHours(3)
                .build();

        when(courseService.getCourses(any())).thenReturn(new PageImpl<>(List.of(course), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].courseCode").value("CP353002"))
                .andExpect(jsonPath("$.content[0].title").value("Software Design"))
                .andExpect(jsonPath("$.content[0].weeklyHours").value(3));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /api/v1/courses/{id}: should return single course")
    void getCourseByIdShouldReturnCourse() throws Exception {
        Course course = Course.builder()
                .id(1L)
                .courseCode("CP353002")
                .title("Software Design")
                .weeklyHours(3)
                .build();

        when(courseService.getCourse(1L)).thenReturn(course);

        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.courseCode").value("CP353002"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/courses: ADMIN can create course")
    void adminCanCreateCourse() throws Exception {
        CourseRequest request = CourseRequest.builder()
                .courseCode("CP353003")
                .title("Software Testing")
                .weeklyHours(3)
                .build();

        Course created = Course.builder()
                .id(2L)
                .courseCode("CP353003")
                .title("Software Testing")
                .weeklyHours(3)
                .build();

        when(courseService.createCourse(any(CourseRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.courseCode").value("CP353003"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("POST /api/v1/courses: Non-ADMIN should be forbidden (403)")
    void nonAdminCannotCreateCourse() throws Exception {
        CourseRequest request = CourseRequest.builder()
                .courseCode("CP353003")
                .title("Software Testing")
                .weeklyHours(3)
                .build();

        mockMvc.perform(post("/api/v1/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/courses/{id}: ADMIN can update course")
    void adminCanUpdateCourse() throws Exception {
        CourseRequest request = CourseRequest.builder()
                .courseCode("CP353002")
                .title("Advanced Software Design")
                .weeklyHours(4)
                .build();

        Course updated = Course.builder()
                .id(1L)
                .courseCode("CP353002")
                .title("Advanced Software Design")
                .weeklyHours(4)
                .build();

        when(courseService.updateCourse(eq(1L), any(CourseRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/courses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Advanced Software Design"))
                .andExpect(jsonPath("$.weeklyHours").value(4));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/courses/{id}: ADMIN can delete course")
    void adminCanDeleteCourse() throws Exception {
        doNothing().when(courseService).deleteCourse(1L);

        mockMvc.perform(delete("/api/v1/courses/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourse(1L);
    }
}

