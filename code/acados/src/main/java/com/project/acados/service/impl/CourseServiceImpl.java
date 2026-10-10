package com.project.acados.service.impl;

import com.project.acados.domain.entity.Course;
import com.project.acados.dto.request.CourseRequest;
import com.project.acados.repository.CourseRepository;
import com.project.acados.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Implementation of CourseService.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §10.2, §16
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Course> getCourses(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found with id: " + id));
    }

    @Override
    public Course createCourse(CourseRequest req) {
        if (courseRepository.existsByCourseCode(req.getCourseCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists: " + req.getCourseCode());
        }

        Course course = Course.builder()
                .courseCode(req.getCourseCode())
                .title(req.getTitle())
                .weeklyHours(req.getWeeklyHours())
                .build();

        return courseRepository.save(course);
    }

    @Override
    public Course updateCourse(Long id, CourseRequest req) {
        Course course = getCourse(id);

        if (!course.getCourseCode().equalsIgnoreCase(req.getCourseCode())
                && courseRepository.existsByCourseCode(req.getCourseCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists: " + req.getCourseCode());
        }

        course.setCourseCode(req.getCourseCode());
        course.setTitle(req.getTitle());
        course.setWeeklyHours(req.getWeeklyHours());

        return courseRepository.save(course);
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = getCourse(id);
        courseRepository.delete(course);
    }
}

