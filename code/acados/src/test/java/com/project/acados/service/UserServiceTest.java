package com.project.acados.service;

import com.project.acados.domain.entity.Student;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.UserCreateRequest;
import com.project.acados.dto.request.UserUpdateRequest;
import com.project.acados.repository.*;
import com.project.acados.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAccountQueryRepository accountQueryRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TeacherSwapRequestRepository swapRequestRepository;

    @Mock
    private UserAccountCleanupRepository accountCleanupRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    private User teacherUser;
    private User studentUser;
    private Teacher teacherProfile;
    private Student studentProfile;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                accountQueryRepository,
                teacherRepository,
                studentRepository,
                registrationRepository,
                scheduleRepository,
                swapRequestRepository,
                accountCleanupRepository,
                passwordEncoder
        );

        teacherUser = User.builder()
                .id(1L)
                .universityId("T001")
                .email("t001@acados.ac.th")
                .passwordHash("hashed")
                .role(UserRole.TEACHER)
                .build();

        studentUser = User.builder()
                .id(2L)
                .universityId("S001")
                .email("s001@acados.ac.th")
                .passwordHash("hashed")
                .role(UserRole.STUDENT)
                .build();

        teacherProfile = Teacher.builder()
                .id(10L)
                .user(teacherUser)
                .fullName("Dr. Somchai")
                .build();

        studentProfile = Student.builder()
                .id(20L)
                .user(studentUser)
                .fullName("Somsak Student")
                .build();
    }

    @Test
    @DisplayName("getUsers: should return paged teacher/student users")
    void testGetUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        when(accountQueryRepository.findByRoleIn(List.of(UserRole.TEACHER, UserRole.STUDENT), pageable))
                .thenReturn(new PageImpl<>(List.of(teacherUser, studentUser)));

        Page<User> result = userService.getUsers(pageable);

        assertThat(result).hasSize(2);
        verify(accountQueryRepository).findByRoleIn(List.of(UserRole.TEACHER, UserRole.STUDENT), pageable);
    }

    @Test
    @DisplayName("getUser: should return user by id")
    void testGetUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacherUser));

        User user = userService.getUser(1L);

        assertThat(user).isNotNull();
        assertThat(user.getUniversityId()).isEqualTo("T001");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getUser: should throw NOT_FOUND if id not found")
    void testGetUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("createUser: should create TEACHER user and teacher profile")
    void testCreateUser_Teacher_Success() {
        UserCreateRequest req = new UserCreateRequest(
                "T002",
                "Dr. New Teacher",
                "t002@acados.ac.th",
                "password123",
                UserRole.TEACHER
        );

        when(userRepository.existsByUniversityId("T002")).thenReturn(false);
        when(userRepository.existsByEmail("t002@acados.ac.th")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });

        User created = userService.createUser(req);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(3L);
        assertThat(created.getRole()).isEqualTo(UserRole.TEACHER);
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    @DisplayName("createUser: should create STUDENT user and student profile")
    void testCreateUser_Student_Success() {
        UserCreateRequest req = new UserCreateRequest(
                "S002",
                "New Student",
                "s002@acados.ac.th",
                "password123",
                UserRole.STUDENT
        );

        when(userRepository.existsByUniversityId("S002")).thenReturn(false);
        when(userRepository.existsByEmail("s002@acados.ac.th")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(4L);
            return u;
        });

        User created = userService.createUser(req);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(4L);
        assertThat(created.getRole()).isEqualTo(UserRole.STUDENT);
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("createUser: should throw BAD_REQUEST when attempting to create ADMIN role")
    void testCreateUser_Admin_Rejected() {
        UserCreateRequest req = new UserCreateRequest(
                "A001",
                "New Admin",
                "a001@acados.ac.th",
                "password123",
                UserRole.ADMIN
        );

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only TEACHER and STUDENT accounts can be created");
    }

    @Test
    @DisplayName("createUser: should throw CONFLICT when universityId exists")
    void testCreateUser_DuplicateUniversityId() {
        UserCreateRequest req = new UserCreateRequest(
                "T001",
                "Duplicate Teacher",
                "other@acados.ac.th",
                "password123",
                UserRole.TEACHER
        );

        when(userRepository.existsByUniversityId("T001")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("University ID already exists");
    }

    @Test
    @DisplayName("updateUser: should update email and profile name")
    void testUpdateUser_Success() {
        UserUpdateRequest req = new UserUpdateRequest("Updated Name", "updated@acados.ac.th", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacherUser));
        when(userRepository.findByEmail("updated@acados.ac.th")).thenReturn(Optional.empty());
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacherProfile));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateUser(1L, req);

        assertThat(updated.getEmail()).isEqualTo("updated@acados.ac.th");
        assertThat(teacherProfile.getFullName()).isEqualTo("Updated Name");
        verify(userRepository).save(teacherUser);
    }

    @Test
    @DisplayName("deleteUser: should reject deletion of ADMIN")
    void testDeleteUser_Admin_Rejected() {
        User adminUser = User.builder().id(99L).role(UserRole.ADMIN).build();
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Admin accounts cannot be deleted");
    }

    @Test
    @DisplayName("deleteUser: should reject deletion of STUDENT if registrations exist")
    void testDeleteUser_StudentWithRegistrations() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(studentRepository.findByUserId(2L)).thenReturn(Optional.of(studentProfile));
        when(registrationRepository.existsByStudentId(20L)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Student has linked registrations");
    }

    @Test
    @DisplayName("deleteUser: should clean up teacher associations and delete teacher user")
    void testDeleteUser_Teacher_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacherUser));
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacherProfile));
        when(scheduleRepository.findByTeacherId(10L)).thenReturn(List.of());
        when(swapRequestRepository.findByTeacherInvolved(10L)).thenReturn(List.of());

        userService.deleteUser(1L);

        verify(accountCleanupRepository).deleteAvailabilities(10L);
        verify(accountCleanupRepository).deletePreferences(10L);
        verify(accountCleanupRepository).deleteQualifications(10L);
        verify(teacherRepository).delete(teacherProfile);
        verify(userRepository).delete(teacherUser);
    }
}

