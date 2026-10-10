package com.project.acados.controller.api;

import com.project.acados.dto.request.LoginRequest;
import com.project.acados.dto.response.AuthResponse;
import com.project.acados.security.TokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.dto.request.ChangePasswordRequest;
import com.project.acados.dto.response.UserProfileResponse;
import com.project.acados.repository.StudentRepository;
import com.project.acados.repository.TeacherRepository;
import com.project.acados.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthApiController(
            AuthenticationManager authenticationManager,
            TokenProvider tokenProvider,
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.universityId(), request.password())
            );
            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring("ROLE_".length()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
            String token = tokenProvider.generateToken(
                    (UserDetails) authentication.getPrincipal()
            );

            // Set browser cookie for seamless page navigation
            Cookie cookie = new Cookie("acados_token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(false);
            cookie.setMaxAge(86400);
            response.addCookie(cookie);

            return new AuthResponse(token, role);
        } catch (org.springframework.security.core.AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid University ID or password");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUniversityId(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String fullName = user.getUniversityId();
        Long teacherId = null;
        Long studentId = null;

        if (user.getRole() == UserRole.TEACHER) {
            var teacherOpt = teacherRepository.findByUserId(user.getId());
            if (teacherOpt.isPresent()) {
                fullName = teacherOpt.get().getFullName();
                teacherId = teacherOpt.get().getId();
            }
        } else if (user.getRole() == UserRole.STUDENT) {
            var studentOpt = studentRepository.findByUserId(user.getId());
            if (studentOpt.isPresent()) {
                fullName = studentOpt.get().getFullName();
                studentId = studentOpt.get().getId();
            }
        } else {
            fullName = "ผู้ดูแลระบบ (Admin)";
        }

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getUniversityId(),
                user.getEmail(),
                user.getRole().name(),
                fullName,
                teacherId,
                studentId
        ));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบ");
        }
        User user = userRepository.findByUniversityId(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลผู้ใช้"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "รหัสผ่านปัจจุบันไม่ถูกต้อง");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("acados_token", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}
