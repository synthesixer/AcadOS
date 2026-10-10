package com.project.acados.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SwapStatus;
import com.project.acados.pattern.observer.ScheduleChangePublisher;
import com.project.acados.repository.*;
import com.project.acados.service.impl.TeacherSwapQueryServiceImpl;
import com.project.acados.service.impl.TeacherSwapServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherSwapServiceTest {

    @Mock
    private TeacherSwapRequestRepository swapRequestRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherQualificationRepository qualificationRepository;

    @Mock
    private TeacherAvailabilityRepository availabilityRepository;

    @Mock
    private ScheduleChangePublisher scheduleChangePublisher;

    private TeacherSwapServiceImpl swapService;
    private TeacherSwapQueryServiceImpl swapQueryService;

    private Teacher teacherA;
    private Teacher teacherB;
    private Course course1;
    private Course course2;
    private Section section1;
    private Section section2;
    private TimeSlot slotA;
    private TimeSlot slotB;
    private Schedule scheduleA;
    private Schedule scheduleB;

    @BeforeEach
    void setUp() {
        swapService = new TeacherSwapServiceImpl(
                swapRequestRepository,
                scheduleRepository,
                teacherRepository,
                qualificationRepository,
                availabilityRepository,
                scheduleChangePublisher
        );
        swapQueryService = new TeacherSwapQueryServiceImpl(swapRequestRepository);

        teacherA = Teacher.builder().id(1L).fullName("Ajarn Somchai").build();
        teacherB = Teacher.builder().id(2L).fullName("Ajarn Somsri").build();

        course1 = Course.builder().id(10L).courseCode("CP353001").title("Software Engineering").build();
        course2 = Course.builder().id(20L).courseCode("CP353002").title("Database Systems").build();

        section1 = Section.builder().id(100L).sectionNumber(1).course(course1).build();
        section2 = Section.builder().id(200L).sectionNumber(1).course(course2).build();

        slotA = TimeSlot.builder()
                .id(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        slotB = TimeSlot.builder()
                .id(2L)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(16, 0))
                .build();

        scheduleA = Schedule.builder()
                .id(1001L)
                .teacher(teacherA)
                .section(section1)
                .timeSlot(slotA)
                .status(ScheduleStatus.PUBLISHED)
                .build();

        scheduleB = Schedule.builder()
                .id(1002L)
                .teacher(teacherB)
                .section(section2)
                .timeSlot(slotB)
                .status(ScheduleStatus.PUBLISHED)
                .build();
    }

    @Test
    @DisplayName("createSwapRequest: should create and save PENDING request when rules pass")
    void testCreateSwapRequest_Success() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacherA));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacherB));
        when(scheduleRepository.findById(1001L)).thenReturn(Optional.of(scheduleA));
        when(scheduleRepository.findById(1002L)).thenReturn(Optional.of(scheduleB));

        when(swapRequestRepository.existsOpenSwapForSchedule(eq(1001L), any())).thenReturn(false);
        when(swapRequestRepository.existsOpenSwapForSchedule(eq(1002L), any())).thenReturn(false);

        when(qualificationRepository.existsByTeacherIdAndCourseId(1L, 20L)).thenReturn(true);
        when(qualificationRepository.existsByTeacherIdAndCourseId(2L, 10L)).thenReturn(true);

        when(availabilityRepository.findByTeacherId(1L)).thenReturn(List.of());
        when(availabilityRepository.findByTeacherId(2L)).thenReturn(List.of());

        when(scheduleRepository.findByTeacherIdAndStatus(1L, ScheduleStatus.PUBLISHED)).thenReturn(List.of(scheduleA));
        when(scheduleRepository.findByTeacherIdAndStatus(1L, ScheduleStatus.DRAFT)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(2L, ScheduleStatus.PUBLISHED)).thenReturn(List.of(scheduleB));
        when(scheduleRepository.findByTeacherIdAndStatus(2L, ScheduleStatus.DRAFT)).thenReturn(List.of());

        when(swapRequestRepository.save(any(TeacherSwapRequest.class))).thenAnswer(inv -> {
            TeacherSwapRequest r = inv.getArgument(0);
            r.setId(500L);
            return r;
        });

        TeacherSwapRequest result = swapService.createSwapRequest(1L, 1001L, 2L, 1002L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getStatus()).isEqualTo(SwapStatus.PENDING);
        assertThat(result.getRequestingTeacher()).isEqualTo(teacherA);
        assertThat(result.getTargetTeacher()).isEqualTo(teacherB);
        verify(swapRequestRepository).save(any(TeacherSwapRequest.class));
    }

    @Test
    @DisplayName("createSwapRequest: should throw CONFLICT if teacher swaps with themselves")
    void testCreateSwapRequest_SelfSwap() {
        assertThatThrownBy(() -> swapService.createSwapRequest(1L, 1001L, 1L, 1002L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("A teacher cannot request a swap with themselves");
    }

    @Test
    @DisplayName("createSwapRequest: should throw CONFLICT if schedule is not published")
    void testCreateSwapRequest_NotPublished() {
        scheduleA.setStatus(ScheduleStatus.DRAFT);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacherA));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacherB));
        when(scheduleRepository.findById(1001L)).thenReturn(Optional.of(scheduleA));
        when(scheduleRepository.findById(1002L)).thenReturn(Optional.of(scheduleB));

        assertThatThrownBy(() -> swapService.createSwapRequest(1L, 1001L, 2L, 1002L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Both schedules must be PUBLISHED");
    }

    @Test
    @DisplayName("createSwapRequest: should throw CONFLICT if teacher lacks qualification")
    void testCreateSwapRequest_NotQualified() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacherA));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacherB));
        when(scheduleRepository.findById(1001L)).thenReturn(Optional.of(scheduleA));
        when(scheduleRepository.findById(1002L)).thenReturn(Optional.of(scheduleB));
        when(swapRequestRepository.existsOpenSwapForSchedule(eq(1001L), any())).thenReturn(false);
        when(swapRequestRepository.existsOpenSwapForSchedule(eq(1002L), any())).thenReturn(false);

        // Teacher A not qualified for Course 2
        when(qualificationRepository.existsByTeacherIdAndCourseId(1L, 20L)).thenReturn(false);

        assertThatThrownBy(() -> swapService.createSwapRequest(1L, 1001L, 2L, 1002L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Teacher is not qualified for the course");
    }

    @Test
    @DisplayName("respondSwap: target teacher accepts swap -> status changes to ACCEPTED")
    void testRespondSwap_Accept() {
        TeacherSwapRequest req = TeacherSwapRequest.builder()
                .id(500L)
                .requestingTeacher(teacherA)
                .requestingSchedule(scheduleA)
                .targetTeacher(teacherB)
                .targetSchedule(scheduleB)
                .status(SwapStatus.PENDING)
                .build();

        when(swapRequestRepository.findById(500L)).thenReturn(Optional.of(req));
        when(qualificationRepository.existsByTeacherIdAndCourseId(1L, 20L)).thenReturn(true);
        when(qualificationRepository.existsByTeacherIdAndCourseId(2L, 10L)).thenReturn(true);
        when(availabilityRepository.findByTeacherId(1L)).thenReturn(List.of());
        when(availabilityRepository.findByTeacherId(2L)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(1L, ScheduleStatus.PUBLISHED)).thenReturn(List.of(scheduleA));
        when(scheduleRepository.findByTeacherIdAndStatus(1L, ScheduleStatus.DRAFT)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(2L, ScheduleStatus.PUBLISHED)).thenReturn(List.of(scheduleB));
        when(scheduleRepository.findByTeacherIdAndStatus(2L, ScheduleStatus.DRAFT)).thenReturn(List.of());

        swapService.respondSwap(500L, 2L, true);

        assertThat(req.getStatus()).isEqualTo(SwapStatus.ACCEPTED);
        assertThat(req.getRespondedAt()).isNotNull();
        verify(swapRequestRepository).save(req);
    }

    @Test
    @DisplayName("respondSwap: target teacher rejects swap -> status changes to REJECTED")
    void testRespondSwap_Reject() {
        TeacherSwapRequest req = TeacherSwapRequest.builder()
                .id(500L)
                .requestingTeacher(teacherA)
                .requestingSchedule(scheduleA)
                .targetTeacher(teacherB)
                .targetSchedule(scheduleB)
                .status(SwapStatus.PENDING)
                .build();

        when(swapRequestRepository.findById(500L)).thenReturn(Optional.of(req));

        swapService.respondSwap(500L, 2L, false);

        assertThat(req.getStatus()).isEqualTo(SwapStatus.REJECTED);
        assertThat(req.getRespondedAt()).isNotNull();
        verify(swapRequestRepository).save(req);
    }

    @Test
    @DisplayName("cancelSwap: requesting teacher can cancel PENDING swap")
    void testCancelSwap_Success() {
        TeacherSwapRequest req = TeacherSwapRequest.builder()
                .id(500L)
                .requestingTeacher(teacherA)
                .requestingSchedule(scheduleA)
                .targetTeacher(teacherB)
                .targetSchedule(scheduleB)
                .status(SwapStatus.PENDING)
                .build();

        when(swapRequestRepository.findById(500L)).thenReturn(Optional.of(req));

        swapService.cancelSwap(500L, 1L);

        assertThat(req.getStatus()).isEqualTo(SwapStatus.CANCELLED);
        verify(swapRequestRepository).save(req);
    }

    @Test
    @DisplayName("approveSwap: admin approves ACCEPTED swap -> teachers swapped & schedules saved")
    void testApproveSwap_Success() {
        TeacherSwapRequest req = TeacherSwapRequest.builder()
                .id(500L)
                .requestingTeacher(teacherA)
                .requestingSchedule(scheduleA)
                .targetTeacher(teacherB)
                .targetSchedule(scheduleB)
                .status(SwapStatus.ACCEPTED)
                .build();

        when(swapRequestRepository.findById(500L)).thenReturn(Optional.of(req));
        when(qualificationRepository.existsByTeacherIdAndCourseId(1L, 20L)).thenReturn(true);
        when(qualificationRepository.existsByTeacherIdAndCourseId(2L, 10L)).thenReturn(true);
        when(availabilityRepository.findByTeacherId(1L)).thenReturn(List.of());
        when(availabilityRepository.findByTeacherId(2L)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(1L, ScheduleStatus.PUBLISHED)).thenReturn(List.of(scheduleA));
        when(scheduleRepository.findByTeacherIdAndStatus(1L, ScheduleStatus.DRAFT)).thenReturn(List.of());
        when(scheduleRepository.findByTeacherIdAndStatus(2L, ScheduleStatus.PUBLISHED)).thenReturn(List.of(scheduleB));
        when(scheduleRepository.findByTeacherIdAndStatus(2L, ScheduleStatus.DRAFT)).thenReturn(List.of());

        swapService.approveSwap(500L);

        assertThat(scheduleA.getTeacher()).isEqualTo(teacherB);
        assertThat(scheduleB.getTeacher()).isEqualTo(teacherA);
        assertThat(req.getStatus()).isEqualTo(SwapStatus.APPROVED);
        assertThat(req.getReviewedAt()).isNotNull();

        verify(scheduleRepository).saveAll(List.of(scheduleA, scheduleB));
        verify(swapRequestRepository).save(req);
        verify(scheduleChangePublisher).notifyObservers(List.of(section1, section2));
    }

    @Test
    @DisplayName("rejectSwap: admin rejects ACCEPTED swap -> status changes to REJECTED")
    void testRejectSwap_Success() {
        TeacherSwapRequest req = TeacherSwapRequest.builder()
                .id(500L)
                .requestingTeacher(teacherA)
                .requestingSchedule(scheduleA)
                .targetTeacher(teacherB)
                .targetSchedule(scheduleB)
                .status(SwapStatus.ACCEPTED)
                .build();

        when(swapRequestRepository.findById(500L)).thenReturn(Optional.of(req));

        swapService.rejectSwap(500L);

        assertThat(req.getStatus()).isEqualTo(SwapStatus.REJECTED);
        assertThat(req.getReviewedAt()).isNotNull();
        verify(swapRequestRepository).save(req);
    }

    @Test
    @DisplayName("swapQueryService: returns formatted inbox response for teacher")
    void testSwapQueryService_GetRequestsForTeacher() {
        TeacherSwapRequest req = TeacherSwapRequest.builder()
                .id(500L)
                .requestingTeacher(teacherA)
                .requestingSchedule(scheduleA)
                .targetTeacher(teacherB)
                .targetSchedule(scheduleB)
                .status(SwapStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(swapRequestRepository.findByTeacherInvolved(1L)).thenReturn(List.of(req));

        var inbox = swapQueryService.getRequestsForTeacher(1L);

        assertThat(inbox.sent()).hasSize(1);
        assertThat(inbox.received()).isEmpty();
        assertThat(inbox.sent().get(0).requestingTeacherName()).isEqualTo("Ajarn Somchai");
        assertThat(inbox.sent().get(0).targetTeacherName()).isEqualTo("Ajarn Somsri");
    }
}
