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
                .map(request -> new SwapResponse(
                        request.getId(),
                        request.getRequestingTeacher().getId(),
                        request.getRequestingSchedule() == null ? null : request.getRequestingSchedule().getId(),
                        request.getTargetTeacher().getId(),
                        request.getTargetSchedule() == null ? null : request.getTargetSchedule().getId(),
                        request.getStatus(),
                        request.getCreatedAt(),
                        request.getRespondedAt(),
                        request.getReviewedAt()
                )).toList();
        List<SwapResponse> received = requests.stream()
                .filter(request -> request.getTargetTeacher().getId().equals(teacherId))
                .map(request -> new SwapResponse(
                        request.getId(),
                        request.getRequestingTeacher().getId(),
                        request.getRequestingSchedule() == null ? null : request.getRequestingSchedule().getId(),
                        request.getTargetTeacher().getId(),
                        request.getTargetSchedule() == null ? null : request.getTargetSchedule().getId(),
                        request.getStatus(),
                        request.getCreatedAt(),
                        request.getRespondedAt(),
                        request.getReviewedAt()
                )).toList();
        return new SwapInboxResponse(sent, received);
    }
}
