package com.project.acados.notification.strategy;

/**
 * Strategy Pattern: one implementation per delivery channel.
 * NotificationService picks the strategies whose supports() matches the requested channels,
 * so a new channel is added with a new class and no change to the service.
 * Reference: class diagram.puml (notification.strategy), Implement_Plan-AcadOS.md §17.2
 */
public interface NotificationStrategy {

    boolean supports(NotificationChannel channel);

    void send(NotificationMessage message);
}
