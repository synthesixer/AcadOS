package com.project.acados.dto.response;

import com.project.acados.domain.entity.Course;
import lombok.*;

/**
 * Response DTO for Course entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §16
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private Long id;
    private String courseCode;
    private String title;
    private Integer weeklyHours;

    public static CourseResponse fromEntity(Course course) {
        if (course == null) {
            return null;
        }
        return CourseResponse.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .title(course.getTitle())
                .weeklyHours(course.getWeeklyHours())
                .build();
    }
}

