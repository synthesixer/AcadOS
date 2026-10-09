package com.project.acados.mapper;

import com.project.acados.domain.entity.Section;
import com.project.acados.dto.response.SectionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper: Section entity to SectionResponse.
 */
@Mapper(componentModel = "spring")
public interface SectionMapper {

    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.courseCode", target = "courseCode")
    @Mapping(source = "course.title", target = "courseTitle")
    SectionResponse toResponse(Section section);

    List<SectionResponse> toResponseList(List<Section> sections);
}
