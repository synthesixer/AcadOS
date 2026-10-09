package com.project.acados.service.impl;

import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.TeacherAvailability;
import com.project.acados.domain.entity.TeacherSwapRequest;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SwapStatus;
import com.project.acados.pattern.observer.ScheduleChangePublisher;
import com.project.acados.repository.ScheduleRepository;
import com.project.acados.repository.TeacherAvailabilityRepository;
import com.project.acados.repository.TeacherQualificationRepository;
import com.project.acados.repository.TeacherRepository;
import com.project.acados.repository.TeacherSwapRequestRepository;
import com.project.acados.service.TeacherSwapService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Implements the documented teacher swap checks and state transitions.
 */
@Service
@Transactional
public class TeacherSwapServiceImpl implements TeacherSwapService {

    private static final List<SwapStatus> OPEN_STATUSES = List.of(SwapStatus.PENDING, SwapStatus.ACCEPTED);
    private static final List<ScheduleStatus> ACTIVE_SCHEDULE_STATUSES =
            List.of(ScheduleStatus.DRAFT, ScheduleStatus.PUBLISHED);

    private final TeacherSwapRequestRepository swapRequestRepository;
    private final ScheduleRepository scheduleRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherQualificationRepository qualificationRepository;
    private final TeacherAvailabilityRepository availabilityRepository;
    private final ScheduleChangePublisher scheduleChangePublisher;

    public TeacherSwapServiceImpl(
            TeacherSwapRequestRepository swapRequestRepository,
            ScheduleRepository scheduleRepository,
            TeacherRepository teacherRepository,
            TeacherQualificationRepository qualificationRepository,
            TeacherAvailabilityRepository availabilityRepository,
            ScheduleChangePublisher scheduleChangePublisher
    ) {
        this.swapRequestRepository = swapRequestRepository;
        this.scheduleRepository = scheduleRepository;
        this.teacherRepository = teacherRepository;
        this.qualificationRepository = qualificationRepository;
        this.availabilityRepository = availabilityRepository;
        this.scheduleChangePublisher = scheduleChangePublisher;
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherSwapRequest createSwapRequest(
            Long requestingTeacherId,
            Long requestingScheduleId,
            Long targetTeacherId,
            Long targetScheduleId
    ) {
        if (Objects.equals(requestingTeacherId, targetTeacherId)) {
            throw conflict("A teacher cannot request a swap with themselves");
        }

        Teacher requestingTeacher = findTeacher(requestingTeacherId);
        Teacher targetTeacher = findTeacher(targetTeacherId);
        Schedule requestingSchedule = findSchedule(requestingScheduleId);
        Schedule targetSchedule = findSchedule(targetScheduleId);

        ensureScheduleBelongsToTeacher(requestingSchedule, requestingTeacherId);
        ensureScheduleBelongsToTeacher(targetSchedule, targetTeacherId);
        ensurePublished(requestingSchedule, targetSchedule);
        ensureDifferentTimeSlots(requestingSchedule, targetSchedule);
        ensureNoOpenSwap(requestingScheduleId, targetScheduleId);
        validateSwapRules(requestingSchedule, targetSchedule, requestingScheduleId, targetScheduleId);

        TeacherSwapRequest request = TeacherSwapRequest.builder()
                .requestingTeacher(requestingTeacher)
                .requestingSchedule(requestingSchedule)
                .targetTeacher(targetTeacher)
                .targetSchedule(targetSchedule)
                .status(SwapStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return swapRequestRepository.save(request);
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public void respondSwap(Long requestId, Long respondingTeacherId, boolean accept) {
        TeacherSwapRequest request = findRequest(requestId);
        ensureStatus(request, SwapStatus.PENDING);
        ensureActor(request.getTargetTeacher(), respondingTeacherId, "Only the target teacher can respond");

        Schedule requestingSchedule = requireSchedule(request.getRequestingSchedule());
        Schedule targetSchedule = requireSchedule(request.getTargetSchedule());
        ensureCurrentScheduleOwners(request, requestingSchedule, targetSchedule);
        ensurePublished(requestingSchedule, targetSchedule);

        if (accept) {
            validateSwapRules(
                    requestingSchedule,
                    targetSchedule,
                    requestingSchedule.getId(),
                    targetSchedule.getId()
            );
            request.accept();
        } else {
            request.reject();
        }

        swapRequestRepository.save(request);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void approveSwap(Long requestId) {
        TeacherSwapRequest request = findRequest(requestId);
        ensureStatus(request, SwapStatus.ACCEPTED);

        Schedule requestingSchedule = requireSchedule(request.getRequestingSchedule());
        Schedule targetSchedule = requireSchedule(request.getTargetSchedule());
        ensureCurrentScheduleOwners(request, requestingSchedule, targetSchedule);
        ensurePublished(requestingSchedule, targetSchedule);
        validateSwapRules(
                requestingSchedule,
                targetSchedule,
                requestingSchedule.getId(),
                targetSchedule.getId()
        );

        Teacher requestingTeacher = request.getRequestingTeacher();
        Teacher targetTeacher = request.getTargetTeacher();
        requestingSchedule.setTeacher(targetTeacher);
        targetSchedule.setTeacher(requestingTeacher);
        scheduleRepository.saveAll(List.of(requestingSchedule, targetSchedule));

        request.approve();
        swapRequestRepository.save(request);
        scheduleChangePublisher.notifyObservers(List.of(
                requestingSchedule.getSection(),
                targetSchedule.getSection()
        ));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void rejectSwap(Long requestId) {
        TeacherSwapRequest request = findRequest(requestId);
        ensureStatus(request, SwapStatus.ACCEPTED);
        request.setStatus(SwapStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        swapRequestRepository.save(request);
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public void cancelSwap(Long requestId, Long requestingTeacherId) {
        TeacherSwapRequest request = findRequest(requestId);
        ensureStatus(request, SwapStatus.PENDING);
        ensureActor(request.getRequestingTeacher(), requestingTeacherId, "Only the requesting teacher can cancel");

        request.cancel();
        swapRequestRepository.save(request);
    }

    private void validateSwapRules(
            Schedule requestingSchedule,
            Schedule targetSchedule,
            Long requestingScheduleId,
            Long targetScheduleId
    ) {
        Teacher requestingTeacher = requestingSchedule.getTeacher();
        Teacher targetTeacher = targetSchedule.getTeacher();

        ensureQualified(requestingTeacher, targetSchedule);
        ensureQualified(targetTeacher, requestingSchedule);
        ensureAvailable(requestingTeacher, targetSchedule);
        ensureAvailable(targetTeacher, requestingSchedule);
        ensureNoScheduleConflict(requestingTeacher.getId(), targetSchedule, requestingScheduleId, targetScheduleId);
        ensureNoScheduleConflict(targetTeacher.getId(), requestingSchedule, requestingScheduleId, targetScheduleId);
    }

    private void ensureNoScheduleConflict(
            Long teacherId,
            Schedule proposedSchedule,
            Long requestingScheduleId,
            Long targetScheduleId
    ) {
        for (ScheduleStatus status : ACTIVE_SCHEDULE_STATUSES) {
            for (Schedule existing : scheduleRepository.findByTeacherIdAndStatus(teacherId, status)) {
                if (Objects.equals(existing.getId(), requestingScheduleId)
                        || Objects.equals(existing.getId(), targetScheduleId)) {
                    continue;
                }

                if (existing.getTimeSlot().overlapsWith(proposedSchedule.getTimeSlot())) {
                    if (status == ScheduleStatus.DRAFT) {
                        throw conflict("DRAFT Timetable Conflict");
                    }
                    throw conflict("Teacher schedule conflict");
                }
            }
        }
    }

    private void ensureQualified(Teacher teacher, Schedule schedule) {
        Long courseId = schedule.getSection().getCourse().getId();
        if (!qualificationRepository.existsByTeacherIdAndCourseId(teacher.getId(), courseId)) {
            throw conflict("Teacher is not qualified for the course");
        }
    }

    private void ensureAvailable(Teacher teacher, Schedule schedule) {
        boolean unavailable = availabilityRepository.findByTeacherId(teacher.getId()).stream()
                .filter(availability -> !availability.isAvailable())
                .map(TeacherAvailability::getTimeSlot)
                .anyMatch(unavailableSlot -> unavailableSlot.overlapsWith(schedule.getTimeSlot()));

        if (unavailable) {
            throw conflict("Teacher is unavailable during the requested time slot");
        }
    }

    private void ensureNoOpenSwap(Long requestingScheduleId, Long targetScheduleId) {
        if (swapRequestRepository.existsOpenSwapForSchedule(requestingScheduleId, OPEN_STATUSES)
                || swapRequestRepository.existsOpenSwapForSchedule(targetScheduleId, OPEN_STATUSES)) {
            throw conflict("An open swap request already exists for one of these schedules");
        }
    }

    private void ensurePublished(Schedule requestingSchedule, Schedule targetSchedule) {
        if (requestingSchedule.getStatus() != ScheduleStatus.PUBLISHED
                || targetSchedule.getStatus() != ScheduleStatus.PUBLISHED) {
            throw conflict("Both schedules must be PUBLISHED");
        }
    }

    private void ensureDifferentTimeSlots(Schedule requestingSchedule, Schedule targetSchedule) {
        if (Objects.equals(requestingSchedule.getTimeSlot().getId(), targetSchedule.getTimeSlot().getId())) {
            throw conflict("Schedules must be in different time slots");
        }
    }

    private void ensureScheduleBelongsToTeacher(Schedule schedule, Long teacherId) {
        if (!Objects.equals(schedule.getTeacher().getId(), teacherId)) {
            throw conflict("Schedule is not assigned to the expected teacher");
        }
    }

    private void ensureCurrentScheduleOwners(
            TeacherSwapRequest request,
            Schedule requestingSchedule,
            Schedule targetSchedule
    ) {
        ensureScheduleBelongsToTeacher(requestingSchedule, request.getRequestingTeacher().getId());
        ensureScheduleBelongsToTeacher(targetSchedule, request.getTargetTeacher().getId());
    }

    private void ensureActor(Teacher expectedTeacher, Long actorTeacherId, String message) {
        if (!Objects.equals(expectedTeacher.getId(), actorTeacherId)) {
            throw conflict(message);
        }
    }

    private void ensureStatus(TeacherSwapRequest request, SwapStatus expectedStatus) {
        if (request.getStatus() != expectedStatus) {
            throw conflict("Swap request must be in " + expectedStatus + " state");
        }
    }

    private Teacher findTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
    }

    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
    }

    private TeacherSwapRequest findRequest(Long requestId) {
        return swapRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Swap request not found"));
    }

    private Schedule requireSchedule(Schedule schedule) {
        if (schedule == null) {
            throw conflict("A schedule was deleted after the swap request was created");
        }
        return schedule;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
