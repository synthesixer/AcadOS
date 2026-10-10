package com.project.acados.service.impl;

import com.project.acados.domain.entity.TeacherSwapRequest;
import com.project.acados.dto.response.SwapInboxResponse;
import com.project.acados.dto.response.SwapResponse;
import com.project.acados.repository.TeacherSwapRequestRepository;
import com.project.acados.service.TeacherSwapQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TeacherSwapQueryServiceImpl implements TeacherSwapQueryService {
    private final TeacherSwapRequestRepository requestRepository;

    public TeacherSwapQueryServiceImpl(TeacherSwapRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Override
    public SwapInboxResponse getRequestsForTeacher(Long teacherId) {
        List<TeacherSwapRequest> requests = requestRepository.findByTeacherInvolved(teacherId).stream()
                .sorted(Comparator.comparing(TeacherSwapRequest::getCreatedAt).reversed())
                .toList();
        List<SwapResponse> sent = requests.stream()
                .filter(request -> request.getRequestingTeacher().getId().equals(teacherId))
                .map(this::mapToResponse)
                .toList();
        List<SwapResponse> received = requests.stream()
                .filter(request -> request.getTargetTeacher().getId().equals(teacherId))
                .map(this::mapToResponse)
                .toList();
        return new SwapInboxResponse(sent, received);
    }

    @Override
    public List<SwapResponse> getAllRequests() {
        return requestRepository.findAll().stream()
                .sorted(Comparator.comparing(TeacherSwapRequest::getCreatedAt).reversed())
                .map(this::mapToResponse)
                .toList();
    }

    private SwapResponse mapToResponse(TeacherSwapRequest request) {
        String reqTeacherName = request.getRequestingTeacher() != null ? request.getRequestingTeacher().getFullName() : null;
        String targetTeacherName = request.getTargetTeacher() != null ? request.getTargetTeacher().getFullName() : null;
        String reqSchedDetails = formatScheduleDetails(request.getRequestingSchedule());
        String targetSchedDetails = formatScheduleDetails(request.getTargetSchedule());

        return new SwapResponse(
                request.getId(),
                request.getRequestingTeacher().getId(),
                request.getRequestingSchedule() == null ? null : request.getRequestingSchedule().getId(),
                request.getTargetTeacher().getId(),
                request.getTargetSchedule() == null ? null : request.getTargetSchedule().getId(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getRespondedAt(),
                request.getReviewedAt(),
                reqTeacherName,
                targetTeacherName,
                reqSchedDetails,
                targetSchedDetails
        );
    }

    private String formatScheduleDetails(com.project.acados.domain.entity.Schedule schedule) {
        if (schedule == null) return "—";
        StringBuilder sb = new StringBuilder();
        if (schedule.getSection() != null && schedule.getSection().getCourse() != null) {
            sb.append(schedule.getSection().getCourse().getCourseCode())
              .append(" ")
              .append(schedule.getSection().getCourse().getTitle())
              .append(" (กลุ่ม ")
              .append(schedule.getSection().getSectionNumber())
              .append(")");
        }
        if (schedule.getTimeSlot() != null) {
            sb.append(" [")
              .append(schedule.getTimeSlot().getDayOfWeek())
              .append(" ")
              .append(schedule.getTimeSlot().getStartTime())
              .append("-")
              .append(schedule.getTimeSlot().getEndTime())
              .append("]");
        }
        if (schedule.getRoom() != null) {
            String bldg = schedule.getRoom().getBuilding() != null ? schedule.getRoom().getBuilding() : "";
            String rm = schedule.getRoom().getRoomNumber() != null ? schedule.getRoom().getRoomNumber() : "";
            sb.append(" ห้อง ").append((bldg + " " + rm).trim());
        }
        return sb.toString().trim();
    }
}
