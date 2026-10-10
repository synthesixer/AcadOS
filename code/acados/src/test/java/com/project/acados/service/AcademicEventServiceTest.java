package com.project.acados.service;

import com.project.acados.domain.entity.AcademicEvent;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.dto.request.AcademicEventRequest;
import com.project.acados.repository.AcademicEventRepository;
import com.project.acados.service.impl.AcademicEventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AcademicEventService Unit Tests")
class AcademicEventServiceTest {

    @Mock
    private AcademicEventRepository academicEventRepository;

    @InjectMocks
    private AcademicEventServiceImpl academicEventService;

    private AcademicEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = AcademicEvent.builder()
                .id(1L)
                .eventName("Semester 1/2026 Registration")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 15))
                .build();
    }

    @Test
    @DisplayName("getEvents should return all events")
    void getEventsReturnsAll() {
        when(academicEventRepository.findAll()).thenReturn(List.of(sampleEvent));

        List<AcademicEvent> result = academicEventService.getEvents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventName()).isEqualTo("Semester 1/2026 Registration");
    }

    @Test
    @DisplayName("getEventsByType should return filtered events")
    void getEventsByTypeReturnsFiltered() {
        when(academicEventRepository.findByEventType(AcademicEventType.REGISTRATION_PERIOD))
                .thenReturn(List.of(sampleEvent));

        List<AcademicEvent> result = academicEventService.getEventsByType(AcademicEventType.REGISTRATION_PERIOD);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventType()).isEqualTo(AcademicEventType.REGISTRATION_PERIOD);
    }

    @Test
    @DisplayName("getEvent by ID should return event if found")
    void getEventReturnsEvent() {
        when(academicEventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

        AcademicEvent result = academicEventService.getEvent(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getEvent by ID should throw 404 if not found")
    void getEventThrowsNotFound() {
        when(academicEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicEventService.getEvent(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    @DisplayName("createEvent should save and return entity when date range is valid")
    void createEventSavesWhenValid() {
        AcademicEventRequest request = AcademicEventRequest.builder()
                .eventName("Semester 1/2026 Registration")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 15))
                .build();

        when(academicEventRepository.save(any(AcademicEvent.class))).thenReturn(sampleEvent);

        AcademicEvent created = academicEventService.createEvent(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(1L);
        verify(academicEventRepository, times(1)).save(any(AcademicEvent.class));
    }

    @Test
    @DisplayName("createEvent should throw 400 Bad Request when endDate is before startDate")
    void createEventThrowsWhenEndDateBeforeStartDate() {
        AcademicEventRequest request = AcademicEventRequest.builder()
                .eventName("Invalid Range")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.of(2026, 6, 15))
                .endDate(LocalDate.of(2026, 6, 1))
                .build();

        assertThatThrownBy(() -> academicEventService.createEvent(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("End date cannot be before start date");

        verify(academicEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteEvent should delete entity if found")
    void deleteEventDeletes() {
        when(academicEventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        doNothing().when(academicEventRepository).delete(sampleEvent);

        academicEventService.deleteEvent(1L);

        verify(academicEventRepository, times(1)).delete(sampleEvent);
    }
}

