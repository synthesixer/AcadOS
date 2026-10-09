package com.project.acados.service.impl;

import com.project.acados.domain.entity.Course;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.dto.request.SectionRequest;
import com.project.acados.exception.BusinessRuleException;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.repository.CourseRepository;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.SectionRepository;
import com.project.acados.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default SectionService. The checks follow Userflow A12-2 (create) and A12-3 (update).
 */
@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Section> getSections(Long courseId) {
        return sectionRepository.findAllWithCourse(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Section getSection(Long id) {
        return sectionRepository.findWithCourseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบ Section"));
    }

    @Override
    @Transactional
    public Section createSection(SectionRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบรายวิชา"));
        if (sectionRepository.existsByCourseIdAndSectionNumber(course.getId(), request.sectionNumber())) {
            throw new BusinessRuleException("Section " + request.sectionNumber() + " ของรายวิชานี้มีอยู่แล้ว");
        }
        return sectionRepository.save(Section.builder()
                .course(course)
                .sectionNumber(request.sectionNumber())
                .capacity(request.capacity())
                .status(SectionStatus.ACTIVE)
                .build());
    }

    @Override
    @Transactional
    public Section updateSection(Long id, SectionRequest request) {
        Section section = sectionRepository.findWithCourseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบ Section"));
        if (section.getStatus() != SectionStatus.ACTIVE) {
            throw new BusinessRuleException("Section นี้ถูกยกเลิกแล้ว ไม่สามารถแก้ไขได้");
        }
        if (request.capacity() < registrationRepository.countBySectionId(id)) {
            throw new BusinessRuleException("BR-05: จำนวนที่นั่งน้อยกว่าจำนวนผู้ลงทะเบียน");
        }
        section.setCapacity(request.capacity());
        return section;
    }
}
