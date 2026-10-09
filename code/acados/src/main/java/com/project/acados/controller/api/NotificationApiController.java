package com.project.acados.controller.api;

import com.project.acados.dto.response.NotificationResponse;
import com.project.acados.mapper.NotificationMapper;
import com.project.acados.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for the signed-in user's in-app notifications.
 * Reference: Implement_Plan-AcadOS.md §16, §14.5
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications of the signed-in user")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @GetMapping
    @Operation(summary = "List the signed-in user's notifications, newest first")
    public List<NotificationResponse> getNotifications(Authentication authentication) {
        return notificationMapper.toResponseList(notificationService.getNotifications(authentication.getName()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark one of the signed-in user's notifications as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
