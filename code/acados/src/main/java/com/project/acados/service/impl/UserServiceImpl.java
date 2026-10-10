package com.project.acados.service.impl;

import com.project.acados.domain.entity.Student;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.UserCreateRequest;
import com.project.acados.dto.request.UserUpdateRequest;
import com.project.acados.repository.*;
import com.project.acados.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserAccountQueryRepository accountQueryRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final RegistrationRepository registrationRepository;
    private final ScheduleRepository scheduleRepository;
    private final TeacherSwapRequestRepository swapRequestRepository;
    private final UserAccountCleanupRepository accountCleanupRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            UserAccountQueryRepository accountQueryRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            RegistrationRepository registrationRepository,
            ScheduleRepository scheduleRepository,
            TeacherSwapRequestRepository swapRequestRepository,
            UserAccountCleanupRepository accountCleanupRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.accountQueryRepository = accountQueryRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.scheduleRepository = scheduleRepository;
        this.swapRequestRepository = swapRequestRepository;
        this.accountCleanupRepository = accountCleanupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        return accountQueryRepository.findByRoleIn(List.of(UserRole.TEACHER, UserRole.STUDENT), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public User createUser(UserCreateRequest request) {
        if (request.role() != UserRole.TEACHER && request.role() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only TEACHER and STUDENT accounts can be created");
        }
        if (userRepository.existsByUniversityId(request.universityId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "University ID already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = userRepository.save(User.builder()
                .universityId(request.universityId())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build());

        if (request.role() == UserRole.TEACHER) {
            teacherRepository.save(Teacher.builder().user(user).fullName(request.fullName()).build());
        } else {
            studentRepository.save(Student.builder().user(user).fullName(request.fullName()).build());
        }
        return user;
    }

    @Override
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = getUser(id);
        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
        });
        user.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (user.getRole() == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
            teacher.setFullName(request.fullName());
        } else if (user.getRole() == UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
            student.setFullName(request.fullName());
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin accounts cannot be updated here");
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = getUser(id);
        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin accounts cannot be deleted");
        }
        if (user.getRole() == UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
            if (registrationRepository.existsByStudentId(student.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Student has linked registrations");
            }
            studentRepository.delete(student);
            studentRepository.flush();
        } else if (user.getRole() == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
            if (!scheduleRepository.findByTeacherId(teacher.getId()).isEmpty()
                    || !swapRequestRepository.findByTeacherInvolved(teacher.getId()).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher has linked schedules or swap requests");
            }
            accountCleanupRepository.deleteAvailabilities(teacher.getId());
            accountCleanupRepository.deletePreferences(teacher.getId());
            accountCleanupRepository.deleteQualifications(teacher.getId());
            teacherRepository.delete(teacher);
            teacherRepository.flush();
        }
        userRepository.delete(user);
        userRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTeacherIdByUniversityId(String universityId) {
        User user = userRepository.findByUniversityId(universityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher role required");
        }
        return teacherRepository.findByUserId(user.getId())
                .map(Teacher::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public String getFullName(User user) {
        if (user.getRole() == UserRole.TEACHER) {
            return teacherRepository.findByUserId(user.getId()).map(Teacher::getFullName).orElse(null);
        }
        if (user.getRole() == UserRole.STUDENT) {
            return studentRepository.findByUserId(user.getId()).map(Student::getFullName).orElse(null);
        }
        return user.getUniversityId();
    }
}
