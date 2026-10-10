package com.project.acados.service;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.SwapStatus;
import com.project.acados.dto.response.SwapInboxResponse;
import com.project.acados.dto.response.SwapResponse;
import com.project.acados.repository.TeacherSwapRequestRepository;
import com.project.acados.service.impl.TeacherSwapQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherSwapQueryService Unit Tests")
class TeacherSwapQueryServiceTest {

    @Mock
    private TeacherSwapRequestRepository requestRepository;

    private TeacherSwapQueryServiceImpl queryService;

    private Teacher teacher1;
    private Teacher teacher2;
    private Schedule schedule1;
    private Schedule schedule2;

    @BeforeEach
    void setUp() {
        queryService = new TeacherSwapQueryServiceImpl(requestRepository);

        teacher1 = Teacher.builder().id(10L).fullName("Ajarn Somchai").build();
        teacher2 = Teacher.builder().id(20L).fullName("Ajarn Somsri").build();

        Course course1 = Course.builder().id(1L).courseCode("CP353001").title("SE").build();
        Section section1 = Section.builder().id(101L).sectionNumber(1).course(course1).build();
        TimeSlot slot1 = TimeSlot.builder().id(1L).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build();
        Room room1 = Room.builder().id(1L).building("SC01").roomNumber("101").build();
        schedule1 = Schedule.builder().id(501L).section(section1).timeSlot(slot1).room(room1).teacher(teacher1).build();

        Course course2 = Course.builder().id(2L).courseCode("CP353002").title("DB").build();
        Section section2 = Section.builder().id(102L).sectionNumber(2).course(course2).build();
        TimeSlot slot2 = TimeSlot.builder().id(2L).dayOfWeek(DayOfWeek.WEDNESDAY).startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(16, 0)).build();
        Room room2 = Room.builder().id(2L).building("SC02").roomNumber("201").build();
        schedule2 = Schedule.builder().id(502L).section(section2).timeSlot(slot2).room(room2).teacher(teacher2).build();
    }

    @Test
    @DisplayName("getRequestsForTeacher should separate sent and received requests sorted by createdAt desc")
    void testGetRequestsForTeacher() {
        TeacherSwapRequest sentReq = TeacherSwapRequest.builder()
                .id(1L)
                .requestingTeacher(teacher1)
                .requestingSchedule(schedule1)
                .targetTeacher(teacher2)
                .targetSchedule(schedule2)
                .status(SwapStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 10, 1, 9, 0))
                .build();

        TeacherSwapRequest receivedReq = TeacherSwapRequest.builder()
                .id(2L)
                .requestingTeacher(teacher2)
                .requestingSchedule(schedule2)
                .targetTeacher(teacher1)
                .targetSchedule(schedule1)
                .status(SwapStatus.APPROVED)
                .createdAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .build();

        when(requestRepository.findByTeacherInvolved(10L)).thenReturn(List.of(sentReq, receivedReq));

        SwapInboxResponse inbox = queryService.getRequestsForTeacher(10L);

        assertThat(inbox.sent()).hasSize(1);
        assertThat(inbox.sent().get(0).id()).isEqualTo(1L);
        assertThat(inbox.sent().get(0).requestingTeacherName()).isEqualTo("Ajarn Somchai");
        assertThat(inbox.sent().get(0).targetTeacherName()).isEqualTo("Ajarn Somsri");
        assertThat(inbox.sent().get(0).requestingScheduleDetails()).contains("CP353001");
        assertThat(inbox.sent().get(0).targetScheduleDetails()).contains("CP353002");

        assertThat(inbox.received()).hasSize(1);
        assertThat(inbox.received().get(0).id()).isEqualTo(2L);
        assertThat(inbox.received().get(0).requestingTeacherName()).isEqualTo("Ajarn Somsri");
    }

    @Test
    @DisplayName("getAllRequests should return all swap requests formatted and sorted desc")
    void testGetAllRequests() {
        TeacherSwapRequest req1 = TeacherSwapRequest.builder()
                .id(1L)
                .requestingTeacher(teacher1)
                .requestingSchedule(schedule1)
                .targetTeacher(teacher2)
                .targetSchedule(schedule2)
                .status(SwapStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 10, 1, 9, 0))
                .build();

        TeacherSwapRequest req2 = TeacherSwapRequest.builder()
                .id(2L)
                .requestingTeacher(teacher2)
                .requestingSchedule(schedule2)
                .targetTeacher(teacher1)
                .targetSchedule(schedule1)
                .status(SwapStatus.APPROVED)
                .createdAt(LocalDateTime.of(2026, 10, 3, 11, 0))
                .build();

        when(requestRepository.findAll()).thenReturn(List.of(req1, req2));

        List<SwapResponse> results = queryService.getAllRequests();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo(2L); // newer first
        assertThat(results.get(1).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("formatScheduleDetails handles null schedules and partial fields gracefully")
    void testFormatScheduleDetails_NullAndPartial() {
        TeacherSwapRequest nullScheduleReq = TeacherSwapRequest.builder()
                .id(3L)
                .requestingTeacher(teacher1)
                .requestingSchedule(null)
                .targetTeacher(teacher2)
                .targetSchedule(null)
                .status(SwapStatus.CANCELLED)
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build();

        Schedule partialSchedule = Schedule.builder().id(999L).build();
        TeacherSwapRequest partialReq = TeacherSwapRequest.builder()
                .id(4L)
                .requestingTeacher(teacher1)
                .requestingSchedule(partialSchedule)
                .targetTeacher(teacher2)
                .targetSchedule(partialSchedule)
                .status(SwapStatus.REJECTED)
                .createdAt(LocalDateTime.of(2026, 10, 2, 10, 0))
                .build();

        when(requestRepository.findAll()).thenReturn(List.of(nullScheduleReq, partialReq));

        List<SwapResponse> results = queryService.getAllRequests();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo(4L); // partialReq (newer)
        assertThat(results.get(0).requestingScheduleDetails()).isEqualTo("");
        assertThat(results.get(1).id()).isEqualTo(3L); // nullScheduleReq (older)
        assertThat(results.get(1).requestingScheduleDetails()).isEqualTo("—");
    }
}
