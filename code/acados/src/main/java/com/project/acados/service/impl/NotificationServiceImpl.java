package com.project.acados.service.impl;

import com.project.acados.domain.entity.Notification;
import com.project.acados.domain.entity.Registration;
import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.notification.strategy.NotificationMessage;
import com.project.acados.notification.strategy.NotificationStrategy;
import com.project.acados.pattern.observer.ScheduleChangeSubject;
import com.project.acados.repository.NotificationRepository;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.service.NotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default NotificationService. Spring injects every NotificationStrategy bean,
 * and each one is asked whether it supports the requested channel.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final List<NotificationStrategy> strategies;
    private final NotificationRepository notificationRepository;
    private final RegistrationRepository registrationRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleChangeSubject scheduleChangeSubject;

    /**
     * Observer Pattern: registers this service with the subject once at startup.
     */
    @PostConstruct
    void attachToScheduleChanges() {
        scheduleChangeSubject.attach(this);
    }

    @Override
    @Transactional
    public void sendNotification(User recipient, NotificationType type, String title, String message,
                                 NotificationChannel... channels) {
        send(recipient, type, title, message, channels);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotificationInNewTransaction(User recipient, NotificationType type, String title,
                                                 String message, NotificationChannel... channels) {
        send(recipient, type, title, message, channels);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String universityId) {
        return notificationRepository.findByUserUniversityIdOrderByCreatedAtDesc(universityId);
    }

    @Override
    @Transactional
    public void markAsRead(String universityId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserUniversityId(notificationId, universityId)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบการแจ้งเตือน"));
        notification.markAsRead();
    }

    /**
     * Called by the subject inside the transaction of the service that changed the timetable
     * (publish, swap approval). Sends one in-app SCHEDULE_CHANGED per changed section to each
     * registered student and each teacher of its PUBLISHED periods.
     */
    @Override
    public void onScheduleChanged(List<Section> sections) {
        if (sections == null) {
            return;
        }
        for (Section section : sections) {
            Map<Long, User> recipients = new LinkedHashMap<>();
            for (Registration registration : registrationRepository.findBySectionId(section.getId())) {
                User student = registration.getStudent().getUser();
                recipients.put(student.getId(), student);
            }
            for (Schedule schedule : scheduleRepository.findBySectionId(section.getId())) {
                if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
                    User teacher = schedule.getTeacher().getUser();
                    recipients.put(teacher.getId(), teacher);
                }
            }
            String label = section.getCourse().getCourseCode() + " Section " + section.getSectionNumber();
            for (User recipient : recipients.values()) {
                send(recipient, NotificationType.SCHEDULE_CHANGED,
                        "ตารางเรียนมีการเปลี่ยนแปลง",
                        "ตารางของ " + label + " มีการเปลี่ยนแปลง กรุณาตรวจสอบตารางล่าสุด",
                        NotificationChannel.IN_APP);
            }
        }
    }

    private void send(User recipient, NotificationType type, String title, String message,
                      NotificationChannel... channels) {
        if (channels == null || channels.length == 0) {
            throw new IllegalArgumentException("At least one notification channel is required");
        }
        Set<NotificationChannel> requestedChannels = EnumSet.copyOf(Arrays.asList(channels));
        NotificationMessage notificationMessage = new NotificationMessage(recipient, type, title, message);

        for (NotificationChannel channel : requestedChannels) {
            for (NotificationStrategy strategy : strategies) {
                if (strategy.supports(channel)) {
                    strategy.send(notificationMessage);
                }
            }
        }
    }
}
