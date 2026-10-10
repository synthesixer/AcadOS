package com.project.acados.service;

import com.project.acados.domain.entity.Course;
import com.project.acados.dto.request.CourseRequest;
import com.project.acados.repository.CourseRepository;
import com.project.acados.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleCourse = Course.builder()
                .id(1L)
                .courseCode("CP353001")
                .title("Software Engineering")
                .weeklyHours(3)
                .build();
    }

    @Test
    @DisplayName("getCourses: should return page of courses")
    void testGetCourses() {
        Pageable pageable = PageRequest.of(0, 10);
        when(courseRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleCourse)));

        Page<Course> result = courseService.getCourses(pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getCourseCode()).isEqualTo("CP353001");
        verify(courseRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getAllCourses: should return all courses list")
    void testGetAllCourses() {
        when(courseRepository.findAll()).thenReturn(List.of(sampleCourse));

        List<Course> result = courseService.getAllCourses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Software Engineering");
        verify(courseRepository).findAll();
    }

    @Test
    @DisplayName("getCourse: should return course when id exists")
    void testGetCourse_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));

        Course result = courseService.getCourse(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCourseCode()).isEqualTo("CP353001");
        verify(courseRepository).findById(1L);
    }

    @Test
    @DisplayName("getCourse: should throw NOT_FOUND when id does not exist")
    void testGetCourse_NotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourse(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Course not found with id: 999");

        verify(courseRepository).findById(999L);
    }

    @Test
    @DisplayName("createCourse: should save and return course when code is unique")
    void testCreateCourse_Success() {
        CourseRequest req = CourseRequest.builder()
                .courseCode("CP353002")
                .title("Database Systems")
                .weeklyHours(3)
                .build();

        when(courseRepository.existsByCourseCode("CP353002")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });

        Course created = courseService.createCourse(req);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getCourseCode()).isEqualTo("CP353002");
        assertThat(created.getTitle()).isEqualTo("Database Systems");
        assertThat(created.getWeeklyHours()).isEqualTo(3);
        verify(courseRepository).existsByCourseCode("CP353002");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("createCourse: should throw CONFLICT when courseCode already exists")
    void testCreateCourse_Conflict() {
        CourseRequest req = CourseRequest.builder()
                .courseCode("CP353001")
                .title("Software Engineering Duplicate")
                .weeklyHours(3)
                .build();

        when(courseRepository.existsByCourseCode("CP353001")).thenReturn(true);

        assertThatThrownBy(() -> courseService.createCourse(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Course code already exists: CP353001");

        verify(courseRepository).existsByCourseCode("CP353001");
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCourse: should update and save course when code is not changed or unique")
    void testUpdateCourse_Success() {
        CourseRequest req = CourseRequest.builder()
                .courseCode("CP353001")
                .title("Advanced Software Engineering")
                .weeklyHours(4)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course updated = courseService.updateCourse(1L, req);

        assertThat(updated.getTitle()).isEqualTo("Advanced Software Engineering");
        assertThat(updated.getWeeklyHours()).isEqualTo(4);
        verify(courseRepository).findById(1L);
        verify(courseRepository).save(sampleCourse);
    }

    @Test
    @DisplayName("updateCourse: should throw CONFLICT when changing to an existing code of another course")
    void testUpdateCourse_Conflict() {
        CourseRequest req = CourseRequest.builder()
                .courseCode("CP353002")
                .title("Software Engineering")
                .weeklyHours(3)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.existsByCourseCode("CP353002")).thenReturn(true);

        assertThatThrownBy(() -> courseService.updateCourse(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Course code already exists: CP353002");

        verify(courseRepository).findById(1L);
        verify(courseRepository).existsByCourseCode("CP353002");
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteCourse: should delete existing course")
    void testDeleteCourse_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));

        courseService.deleteCourse(1L);

        verify(courseRepository).findById(1L);
        verify(courseRepository).delete(sampleCourse);
    }

    @Test
    @DisplayName("deleteCourse: should throw NOT_FOUND when course does not exist")
    void testDeleteCourse_NotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Course not found with id: 999");

        verify(courseRepository).findById(999L);
        verify(courseRepository, never()).delete(any());
    }
}

