package com.project.acados.service.impl;

import com.project.acados.domain.entity.AcademicEvent;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.dto.request.AcademicEventRequest;
import com.project.acados.repository.AcademicEventRepository;
import com.project.acados.service.AcademicEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Implementation of AcademicEventService.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §16, database.md §2.15
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AcademicEventServiceImpl implements AcademicEventService {

    private final AcademicEventRepository academicEventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AcademicEvent> getEvents() {
        return academicEventRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicEvent> getEventsByType(AcademicEventType type) {
        if (type == null) {
            return academicEventRepository.findAll();
        }
        return academicEventRepository.findByEventType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicEvent getEvent(Long id) {
        return academicEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic event not found with id: " + id));
    }

    @Override
    public AcademicEvent createEvent(AcademicEventRequest req) {
        validateDateRange(req);

        AcademicEvent event = AcademicEvent.builder()
                .eventName(req.getEventName())
                .eventType(req.getEventType())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();

        return academicEventRepository.save(event);
    }

    @Override
    public AcademicEvent updateEvent(Long id, AcademicEventRequest req) {
        AcademicEvent event = getEvent(id);
        validateDateRange(req);

        event.setEventName(req.getEventName());
        event.setEventType(req.getEventType());
        event.setStartDate(req.getStartDate());
        event.setEndDate(req.getEndDate());

        return academicEventRepository.save(event);
    }

    @Override
    public void deleteEvent(Long id) {
        AcademicEvent event = getEvent(id);
        academicEventRepository.delete(event);
    }

    private void validateDateRange(AcademicEventRequest req) {
        if (req.getEndDate() != null && req.getStartDate() != null && req.getEndDate().isBefore(req.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
    }
}

