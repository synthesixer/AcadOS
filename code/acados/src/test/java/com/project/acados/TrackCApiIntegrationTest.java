package com.project.acados;

import com.jayway.jsonpath.JsonPath;
import com.project.acados.domain.entity.*;
import com.project.acados.domain.enums.AcademicEventType;
import com.project.acados.domain.enums.NotificationType;
import com.project.acados.domain.enums.ScheduleStatus;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.domain.enums.SwapStatus;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.exception.BusinessRuleException;
import com.project.acados.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests of the Track C API: controller, service, repository and the test database,
 * without a test transaction, so lazy loading and transaction boundaries behave as in production.
 * Each test creates its own data and deletes it afterwards.
 */
@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrackCApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AcademicEventRepository academicEventRepository;
    @Autowired
    private TeacherSwapRequestRepository swapRequestRepository;

    private Student student;
    private Teacher teacher;
    private Course softwareDesign;
    private Section softwareDesignSection;
    private Section databaseSection;
    private Schedule softwareDesignPeriod;
    private Schedule databasePeriod;

    @BeforeEach
    void setUp() {
        deleteAllData();

        User studentUser = userRepository.save(user("S001", UserRole.STUDENT));
        User teacherUser = userRepository.save(user("T001", UserRole.TEACHER));
        userRepository.save(user("admin", UserRole.ADMIN));
        student = studentRepository.save(Student.builder().user(studentUser).fullName("Student One").build());
        teacher = teacherRepository.save(Teacher.builder().user(teacherUser).fullName("Teacher One").build());

        softwareDesign = courseRepository.save(Course.builder()
                .courseCode("CP353002").title("Software Design").weeklyHours(3).build());
        Course database = courseRepository.save(Course.builder()
                .courseCode("CP353001").title("Database Systems").weeklyHours(3).build());
        softwareDesignSection = sectionRepository.save(Section.builder()
                .course(softwareDesign).sectionNumber(1).capacity(40).build());
        databaseSection = sectionRepository.save(Section.builder()
                .course(database).sectionNumber(1).capacity(40).build());

        // The two sections overlap on Monday 10:00-12:00.
        TimeSlot mondayMorning = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).build());
        TimeSlot mondayLateMorning = timeSlotRepository.save(TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(13, 0)).build());
        softwareDesignPeriod = scheduleRepository.save(Schedule.builder()
                .section(softwareDesignSection).teacher(teacher).timeSlot(mondayMorning)
                .status(ScheduleStatus.PUBLISHED).build());
        databasePeriod = scheduleRepository.save(Schedule.builder()
                .section(databaseSection).teacher(teacher).timeSlot(mondayLateMorning)
                .status(ScheduleStatus.PUBLISHED).build());

        academicEventRepository.save(AcademicEvent.builder()
                .eventName("Registration")
                .eventType(AcademicEventType.REGISTRATION_PERIOD)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build());
    }

    @AfterEach
    void tearDown() {
        deleteAllData();
    }

    private void deleteAllData() {
        notificationRepository.deleteAllInBatch();
        registrationRepository.deleteAllInBatch();
        swapRequestRepository.deleteAllInBatch();
        scheduleRepository.deleteAllInBatch();
        sectionRepository.deleteAllInBatch();
        academicEventRepository.deleteAllInBatch();
        timeSlotRepository.deleteAllInBatch();
        teacherRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private User user(String universityId, UserRole role) {
        return User.builder()
                .universityId(universityId)
                .email(universityId.toLowerCase() + "@acados.local")
                .passwordHash("hashed")
                .role(role)
                .build();
    }

    private String registrationBody(Section section) {
        return "{\"sectionId\": " + section.getId() + "}";
    }

    private List<NotificationType> notificationTypesOf(String universityId) {
        return notificationRepository.findByUserUniversityIdOrderByCreatedAtDesc(universityId).stream()
                .map(Notification::getType)
                .toList();
    }

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void studentRegistersThenSeesRegistrationAndNotification() throws Exception {
        mockMvc.perform(get("/api/v1/sections").param("courseId", softwareDesign.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseCode").value("CP353002"));

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(softwareDesignSection)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("CP353002"))
                .andExpect(jsonPath("$.studentName").value("Student One"))
                .andExpect(jsonPath("$.registeredAt").exists());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        mockMvc.perform(get("/api/v1/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sectionId").value(softwareDesignSection.getId()));

        String notifications = mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("REGISTRATION_SUCCESS"))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andReturn().getResponse().getContentAsString();
        Number notificationId = JsonPath.read(notifications, "$[0].id");

        mockMvc.perform(put("/api/v1/notifications/" + notificationId + "/read"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(jsonPath("$[0].isRead").value(true));
    }

    @Test
    @WithMockUser(username = "S001", roles = "STUDENT")
    void scheduleConflictRejectsRegistrationButKeepsConflictNotification() throws Exception {
        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(softwareDesignSection)))
                .andExpect(status().isCreated());

        // No GlobalExceptionHandler exists yet, so the BusinessRuleException reaches the test.
        assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationBody(databaseSection))))
                .hasRootCauseInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-03");

        assertThat(registrationRepository.count()).isEqualTo(1);
        assertThat(notificationTypesOf("S001"))
                .containsExactlyInAnyOrder(NotificationType.CONFLICT_DETECTED, NotificationType.REGISTRATION_SUCCESS);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCancelsSectionWithRegistrationPeriodAndOpenSwapRequest() throws Exception {
        registrationRepository.save(Registration.builder()
                .student(student).section(softwareDesignSection).course(softwareDesign).build());
        TeacherSwapRequest openSwap = swapRequestRepository.save(TeacherSwapRequest.builder()
                .requestingTeacher(teacher)
                .requestingSchedule(softwareDesignPeriod)
                .targetTeacher(teacher)
                .targetSchedule(databasePeriod)
                .status(SwapStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/v1/sections/" + softwareDesignSection.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(sectionRepository.findById(softwareDesignSection.getId()).orElseThrow().getStatus())
                .isEqualTo(SectionStatus.CANCELLED);
        assertThat(registrationRepository.countBySectionId(softwareDesignSection.getId())).isZero();
        assertThat(scheduleRepository.findBySectionId(softwareDesignSection.getId())).isEmpty();
        assertThat(scheduleRepository.findBySectionId(databaseSection.getId())).hasSize(1);

        TeacherSwapRequest keptSwap = swapRequestRepository.findById(openSwap.getId()).orElseThrow();
        assertThat(keptSwap.getStatus()).isEqualTo(SwapStatus.CANCELLED);
        assertThat(keptSwap.getRequestingSchedule()).isNull();
        assertThat(keptSwap.getTargetSchedule()).isNotNull();

        assertThat(notificationTypesOf("S001")).containsExactly(NotificationType.SECTION_CANCELLED);
        assertThat(notificationTypesOf("T001")).containsExactly(NotificationType.SECTION_CANCELLED);
    }

    @Test
    void swaggerDocumentationIsPublicAndDescribesTrackCEndpointsWithJwtScheme() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AcadOS API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/registrations']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sections/{id}/cancel']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/notifications/{id}/read']").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCreatesUpdatesAndListsSections() throws Exception {
        String created = mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\": " + softwareDesign.getId() + ", \"sectionNumber\": 2, \"capacity\": 30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.courseTitle").value("Software Design"))
                .andReturn().getResponse().getContentAsString();
        Number sectionId = JsonPath.read(created, "$.id");

        mockMvc.perform(put("/api/v1/sections/" + sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\": 35}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(35))
                .andExpect(jsonPath("$.sectionNumber").value(2));

        mockMvc.perform(get("/api/v1/sections/" + sectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(35));

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
