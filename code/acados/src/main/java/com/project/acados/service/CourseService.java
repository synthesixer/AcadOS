package com.project.acados.service;

import com.project.acados.domain.entity.Course;
import com.project.acados.dto.request.CourseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Course management.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §10.2, §16
 */
public interface CourseService {

    Page<Course> getCourses(Pageable pageable);

    List<Course> getAllCourses();

    Course getCourse(Long id);

    Course createCourse(CourseRequest req);

    Course updateCourse(Long id, CourseRequest req);

    void deleteCourse(Long id);
}

