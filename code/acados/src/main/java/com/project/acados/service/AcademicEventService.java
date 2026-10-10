package com.project.acados.service;

import com.project.acados.domain.entity.AcademicEvent;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.dto.request.AcademicEventRequest;

import java.util.List;

/**
 * Service interface for AcademicEvent calendar management.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §16
 */
public interface AcademicEventService {

    List<AcademicEvent> getEvents();

    List<AcademicEvent> getEventsByType(AcademicEventType type);

    AcademicEvent getEvent(Long id);

    AcademicEvent createEvent(AcademicEventRequest req);

    AcademicEvent updateEvent(Long id, AcademicEventRequest req);

    void deleteEvent(Long id);
}

