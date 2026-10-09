package com.project.acados.mapper;

import com.project.acados.domain.entity.Notification;
import com.project.acados.dto.response.NotificationResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper: Notification entity to NotificationResponse.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}
