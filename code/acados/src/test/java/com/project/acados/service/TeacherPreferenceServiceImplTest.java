package com.project.acados.service;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.TeacherAvailabilityToggleRequest;
import com.project.acados.dto.request.TeacherPreferenceRequest;
import com.project.acados.dto.response.CourseResponse;
import com.project.acados.dto.response.TeacherAvailabilityResponse;
import com.project.acados.dto.response.TeacherPreferenceResponse;
import com.project.acados.repository.*;
import com.project.acados.service.impl.TeacherPreferenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherPreferenceServiceImpl Unit Tests")
class TeacherPreferenceServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherPreferenceRepository teacherPreferenceRepository;

    @Mock
    private TeacherQualificationRepository teacherQualificationRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private TeacherPreferenceServiceImpl service;

    private User teacherUser;
    private Teacher teacher;
    private Course course;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder().id(1L).universityId("T001").email("t001@kku.ac.th").role(UserRole.TEACHER).build();
        teacher = Teacher.builder().id(10L).fullName("Ajarn Somchai").user(teacherUser).build();
        course = Course.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(3).build();
    }

    private void mockResolveTeacher() {
        when(userRepository.findByUniversityId("T001")).thenReturn(Optional.of(teacherUser));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
    }

    @Test
    @DisplayName("getPreferences: returns sorted preferences for teacher")
    void testGetPreferences() {
        mockResolveTeacher();
        TeacherPreference pref = TeacherPreference.builder().id(100L).teacher(teacher).course(course).priority(1).build();
        when(teacherPreferenceRepository.findByTeacherIdOrderByPriorityAsc(10L)).thenReturn(List.of(pref));

        List<TeacherPreferenceResponse> result = service.getPreferences("T001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).courseCode()).isEqualTo("CP353002");
    }

    @Test
    @DisplayName("getQualifiedCourses: returns qualified course list")
    void testGetQualifiedCourses() {
        mockResolveTeacher();
        TeacherQualification tq = TeacherQualification.builder().id(1L).teacher(teacher).course(course).build();
        when(teacherQualificationRepository.findByTeacherId(10L)).thenReturn(List.of(tq));

        List<CourseResponse> result = service.getQualifiedCourses("T001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseCode()).isEqualTo("CP353002");
    }

    @Test
    @DisplayName("savePreference: succeeds when qualified (BR-06)")
    void testSavePreference_Qualified() {
        mockResolveTeacher();
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(true);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(teacherPreferenceRepository.findByTeacherId(10L)).thenReturn(List.of());

        TeacherPreference pref = TeacherPreference.builder().id(100L).teacher(teacher).course(course).priority(1).build();
        when(teacherPreferenceRepository.save(any(TeacherPreference.class))).thenReturn(pref);

        TeacherPreferenceResponse result = service.savePreference(new TeacherPreferenceRequest(5L, 1), "T001");

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.priority()).isEqualTo(1);
    }

    @Test
    @DisplayName("savePreference: throws 400 when unqualified (BR-06)")
    void testSavePreference_Unqualified() {
        mockResolveTeacher();
        when(teacherQualificationRepository.existsByTeacherIdAndCourseId(10L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> service.savePreference(new TeacherPreferenceRequest(5L, 1), "T001"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("BR-06");
    }

    @Test
    @DisplayName("deletePreference: deletes successfully when owner")
    void testDeletePreference_Owner() {
        mockResolveTeacher();
        TeacherPreference pref = TeacherPreference.builder().id(100L).teacher(teacher).course(course).priority(1).build();
        when(teacherPreferenceRepository.findById(100L)).thenReturn(Optional.of(pref));

        service.deletePreference(100L, "T001");

        verify(teacherPreferenceRepository).delete(pref);
    }

    @Test
    @DisplayName("deletePreference: throws 403 when not owner")
    void testDeletePreference_NotOwner() {
        mockResolveTeacher();
        Teacher other = Teacher.builder().id(99L).build();
        TeacherPreference pref = TeacherPreference.builder().id(100L).teacher(other).course(course).priority(1).build();
        when(teacherPreferenceRepository.findById(100L)).thenReturn(Optional.of(pref));

        assertThatThrownBy(() -> service.deletePreference(100L, "T001"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("getAvailabilities: returns mapped availability list")
    void testGetAvailabilities() {
        mockResolveTeacher();
        TimeSlot slot = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        when(timeSlotRepository.findAll()).thenReturn(List.of(slot));
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(10L, 1L)).thenReturn(Optional.empty());

        List<TeacherAvailabilityResponse> result = service.getAvailabilities("T001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAvailable()).isTrue();
    }

    @Test
    @DisplayName("updateAvailability: updates slot and returns response")
    void testUpdateAvailability() {
        mockResolveTeacher();
        TimeSlot slot = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        TeacherAvailability avail = TeacherAvailability.builder().id(50L).teacher(teacher).timeSlot(slot).isAvailable(false).build();

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(teacherAvailabilityRepository.findByTeacherIdAndTimeSlotId(10L, 1L)).thenReturn(Optional.empty());
        when(teacherAvailabilityRepository.save(any(TeacherAvailability.class))).thenReturn(avail);

        TeacherAvailabilityResponse result = service.updateAvailability(new TeacherAvailabilityToggleRequest(1L, false), "T001");

        assertThat(result.isAvailable()).isFalse();
    }
}

