package com.project.acados.service;

import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.dto.request.SectionRequest;
import com.project.acados.exception.BusinessRuleException;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.repository.CourseRepository;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.SectionRepository;
import com.project.acados.service.impl.SectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private SectionServiceImpl sectionService;

    private Course course;
    private Section section;

    @BeforeEach
    void setUp() {
        course = Course.builder().id(5L).courseCode("CP353002").title("Software Design").weeklyHours(3).build();
        section = Section.builder().id(100L).course(course).sectionNumber(1).capacity(40).build();
    }

    @Test
    void getSections_returnsSectionsOfTheCourse() {
        when(sectionRepository.findAllWithCourse(5L)).thenReturn(List.of(section));

        assertThat(sectionService.getSections(5L)).containsExactly(section);
    }

    @Test
    void getSection_whenMissing_throwsResourceNotFoundException() {
        when(sectionRepository.findWithCourseById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.getSection(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createSection_savesActiveSectionOfTheCourse() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(sectionRepository.existsByCourseIdAndSectionNumber(5L, 2)).thenReturn(false);
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Section created = sectionService.createSection(new SectionRequest(5L, 2, 30));

        assertThat(created.getCourse()).isSameAs(course);
        assertThat(created.getSectionNumber()).isEqualTo(2);
        assertThat(created.getCapacity()).isEqualTo(30);
        assertThat(created.getStatus()).isEqualTo(SectionStatus.ACTIVE);
    }

    @Test
    void createSection_whenCourseMissing_throwsResourceNotFoundException() {
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.createSection(new SectionRequest(9L, 1, 30)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_whenSectionNumberExistsInCourse_throwsBusinessRuleException() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(sectionRepository.existsByCourseIdAndSectionNumber(5L, 1)).thenReturn(true);

        assertThatThrownBy(() -> sectionService.createSection(new SectionRequest(5L, 1, 30)))
                .isInstanceOf(BusinessRuleException.class);

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateSection_changesCapacityOnly() {
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(registrationRepository.countBySectionId(100L)).thenReturn(10L);

        Section updated = sectionService.updateSection(100L, new SectionRequest(99L, 7, 25));

        assertThat(updated.getCapacity()).isEqualTo(25);
        assertThat(updated.getSectionNumber()).isEqualTo(1);
        assertThat(updated.getCourse()).isSameAs(course);
    }

    @Test
    void updateSection_whenCapacityEqualsRegisteredCount_isAllowed() {
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(registrationRepository.countBySectionId(100L)).thenReturn(25L);

        Section updated = sectionService.updateSection(100L, new SectionRequest(null, null, 25));

        assertThat(updated.getCapacity()).isEqualTo(25);
    }

    @Test
    void updateSection_whenCapacityBelowRegisteredCount_throwsBR05() {
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));
        when(registrationRepository.countBySectionId(100L)).thenReturn(26L);

        assertThatThrownBy(() -> sectionService.updateSection(100L, new SectionRequest(null, null, 25)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-05");

        assertThat(section.getCapacity()).isEqualTo(40);
    }

    @Test
    void updateSection_whenSectionCancelled_throwsBusinessRuleException() {
        section.setStatus(SectionStatus.CANCELLED);
        when(sectionRepository.findWithCourseById(100L)).thenReturn(Optional.of(section));

        assertThatThrownBy(() -> sectionService.updateSection(100L, new SectionRequest(null, null, 25)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateSection_whenMissing_throwsResourceNotFoundException() {
        when(sectionRepository.findWithCourseById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.updateSection(999L, new SectionRequest(null, null, 25)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
