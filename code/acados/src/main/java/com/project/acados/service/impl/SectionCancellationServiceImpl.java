package com.project.acados.service.impl;

import com.project.acados.domain.entity.Registration;
import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.TeacherSwapRequest;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.SwapStatus;
import com.project.acados.exception.ResourceNotFoundException;
import com.project.acados.notification.strategy.NotificationChannel;
import com.project.acados.repository.RegistrationRepository;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.SectionRepository;
import com.project.acados.repository.TeacherSwapRequestRepository;
import com.project.acados.service.NotificationService;
import com.project.acados.service.SectionCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default SectionCancellationService. The steps follow Userflow A08.
 */
@Service
@RequiredArgsConstructor
public class SectionCancellationServiceImpl implements SectionCancellationService {

    private final SectionRepository sectionRepository;
    private final RegistrationRepository registrationRepository;
    private final ScheduleRepository scheduleRepository;
    private final TeacherSwapRequestRepository swapRequestRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void cancelSection(Long sectionId) {
        Section section = sectionRepository.findWithCourseById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบ Section"));
        section.cancel();

        List<Schedule> periods = scheduleRepository.findBySectionId(sectionId);
        Set<Long> periodIds = periods.stream().map(Schedule::getId).collect(Collectors.toSet());

        Map<Long, User> recipients = new LinkedHashMap<>();
        for (Registration registration : registrationRepository.findBySectionId(sectionId)) {
            addRecipient(recipients, registration.getStudent().getUser());
        }
        for (Schedule period : periods) {
            addRecipient(recipients, period.getTeacher().getUser());
        }
        for (TeacherSwapRequest swapRequest : swapRequestRepository.findAll()) {
            if (releasePeriods(swapRequest, periodIds)) {
                addRecipient(recipients, swapRequest.getRequestingTeacher().getUser());
                addRecipient(recipients, swapRequest.getTargetTeacher().getUser());
            }
        }

        registrationRepository.deleteBySectionId(sectionId);
        scheduleRepository.deleteAll(periods);

        String label = section.getCourse().getCourseCode() + " Section " + section.getSectionNumber();
        for (User recipient : recipients.values()) {
            notificationService.sendNotification(recipient,
                    NotificationType.SECTION_CANCELLED,
                    "Section ถูกยกเลิก",
                    label + " ถูกยกเลิกแล้ว การลงทะเบียนและคาบสอนของ Section นี้ถูกลบออก",
                    NotificationChannel.IN_APP);
        }
    }

    /**
     * Detaches the swap request from the periods that are about to be deleted, so its row is kept
     * as audit history (database.md §2.13), and cancels it when it was still open.
     *
     * @return true when the swap request referred to one of the periods
     */
    private boolean releasePeriods(TeacherSwapRequest swapRequest, Set<Long> periodIds) {
        boolean affected = false;
        Schedule requestingPeriod = swapRequest.getRequestingSchedule();
        if (requestingPeriod != null && periodIds.contains(requestingPeriod.getId())) {
            swapRequest.setRequestingSchedule(null);
            affected = true;
        }
        Schedule targetPeriod = swapRequest.getTargetSchedule();
        if (targetPeriod != null && periodIds.contains(targetPeriod.getId())) {
            swapRequest.setTargetSchedule(null);
            affected = true;
        }
        if (affected && (swapRequest.getStatus() == SwapStatus.PENDING
                || swapRequest.getStatus() == SwapStatus.ACCEPTED)) {
            swapRequest.cancel();
        }
        return affected;
    }

    private void addRecipient(Map<Long, User> recipients, User user) {
        recipients.put(user.getId(), user);
    }
}
