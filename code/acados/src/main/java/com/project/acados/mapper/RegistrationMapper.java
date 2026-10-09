package com.project.acados.mapper;

import com.project.acados.domain.entity.Registration;
import com.project.acados.dto.response.RegistrationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper: Registration entity to RegistrationResponse.
 */
@Mapper(componentModel = "spring")
public interface RegistrationMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "student.fullName", target = "studentName")
    @Mapping(source = "section.id", target = "sectionId")
    @Mapping(source = "section.sectionNumber", target = "sectionNumber")
    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.courseCode", target = "courseCode")
    @Mapping(source = "course.title", target = "courseTitle")
    RegistrationResponse toResponse(Registration registration);

    List<RegistrationResponse> toResponseList(List<Registration> registrations);
}
