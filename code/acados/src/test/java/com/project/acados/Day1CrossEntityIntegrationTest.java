package com.project.acados;

import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.*;
import com.project.acados.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Day1CrossEntityIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private TeacherSwapRequestRepository swapRequestRepository;

    @Test
    @DisplayName("End-to-End Chain: สร้าง User -> Teacher/Student -> Course/Sec -> Room/Slot -> Schedule -> Reg -> Swap")
    void testFullIntegratedDomainLifecycle() {
        // 1. [Person 2] สร้าง User และ Teacher (ครู A และ ครู B)
        User userTeacherA = userRepository.save(User.builder().universityId("T001").email("teaA@kku.ac.th").passwordHash("pass").role(UserRole.TEACHER).build());
        Teacher teacherA = teacherRepository.save(Teacher.builder().user(userTeacherA).fullName("อ.พุฒิเมธ").build());

        User userTeacherB = userRepository.save(User.builder().universityId("T002").email("teaB@kku.ac.th").passwordHash("pass").role(UserRole.TEACHER).build());
        Teacher teacherB = teacherRepository.save(Teacher.builder().user(userTeacherB).fullName("อ.วงศกร").build());

        // 2. [Person 3] สร้าง User และ Student
        User userStudent = userRepository.save(User.builder().universityId("S001").email("std@kku.ac.th").passwordHash("pass").role(UserRole.STUDENT).build());
        Student student = studentRepository.save(Student.builder().user(userStudent).fullName("นายจิรภัทร").build());

        // 3. [Person 1] สร้าง Course
        Course course = courseRepository.save(Course.builder().courseCode("CP353002").title("SQA").weeklyHours(3).build());

        // 4. [Person 3] สร้าง Section
        Section section = sectionRepository.save(Section.builder().course(course).sectionNumber(1).capacity(50).status(SectionStatus.ACTIVE).build());

        // 5. [Person 1] สร้าง Room และ TimeSlot
        Room room = roomRepository.save(Room.builder().building("SC01").roomNumber("301").floor(3).capacity(50).isAvailable(true).build());
        TimeSlot slotA = timeSlotRepository.save(TimeSlot.builder().dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build());
        TimeSlot slotB = timeSlotRepository.save(TimeSlot.builder().dayOfWeek(DayOfWeek.WEDNESDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build());

        // 6. [Person 1] สร้าง Schedule สำหรับทั้งสองคาบ
        Schedule scheduleA = scheduleRepository.save(Schedule.builder().section(section).teacher(teacherA).room(room).timeSlot(slotA).status(ScheduleStatus.PUBLISHED).build());
        Schedule scheduleB = scheduleRepository.save(Schedule.builder().section(section).teacher(teacherB).room(room).timeSlot(slotB).status(ScheduleStatus.PUBLISHED).build());

        // 7. [Person 3] สร้าง Registration
        Registration registration = registrationRepository.save(Registration.builder().student(student).section(section).course(course).registeredAt(LocalDateTime.now()).build());

        // 8. [Person 2] สร้าง TeacherSwapRequest (Snapshot Audit Log + SET NULL)
        TeacherSwapRequest swapRequest = swapRequestRepository.save(TeacherSwapRequest.builder()
                .requestingTeacher(teacherA)
                .requestingSchedule(scheduleA)
                .targetTeacher(teacherB)
                .targetSchedule(scheduleB)
                .status(SwapStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());

        // ตรวจสอบความถูกต้องว่าทุก Entity ถูกจัดเก็บและเชื่อมโยงกันอย่างสมบูรณ์
        assertThat(scheduleA.getId()).isNotNull();
        assertThat(scheduleB.getId()).isNotNull();
        assertThat(registration.getId()).isNotNull();
        assertThat(registration.getStudent().getFullName()).isEqualTo("นายจิรภัทร");
        assertThat(registration.getSection().getSectionNumber()).isEqualTo(1);

        // ตรวจสอบ TeacherSwapRequest และ SwapStatus ในสถานะเริ่มต้น PENDING
        assertThat(swapRequest.getId()).isNotNull();
        assertThat(swapRequest.getRequestingTeacher().getFullName()).isEqualTo("อ.พุฒิเมธ");
        assertThat(swapRequest.getTargetTeacher().getFullName()).isEqualTo("อ.วงศกร");
        assertThat(swapRequest.getStatus()).isEqualTo(SwapStatus.PENDING);

        // ทดสอบ State Transitions: ครู B ตอบรับ (ACCEPTED) -> Admin อนุมัติ (APPROVED)
        swapRequest.accept();
        swapRequest = swapRequestRepository.save(swapRequest);
        assertThat(swapRequest.getStatus()).isEqualTo(SwapStatus.ACCEPTED);
        assertThat(swapRequest.getRespondedAt()).isNotNull();

        swapRequest.approve();
        swapRequest = swapRequestRepository.save(swapRequest);
        assertThat(swapRequest.getStatus()).isEqualTo(SwapStatus.APPROVED);
        assertThat(swapRequest.getReviewedAt()).isNotNull();
    }
}