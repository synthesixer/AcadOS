package com.project.acados.controller.api;

import com.project.acados.domain.entity.Registration;
import com.project.acados.dto.request.RegistrationRequest;
import com.project.acados.dto.response.RegistrationResponse;
import com.project.acados.mapper.RegistrationMapper;
import com.project.acados.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST API for student course registration.
 * Reference: Implement_Plan-AcadOS.md §16, Sequence_diagram.md 04
 */
@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
@Tag(name = "Registrations", description = "Student course registration and withdrawal")
public class RegistrationApiController {

    private final RegistrationService registrationService;
    private final RegistrationMapper registrationMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "List registrations: a student sees their own, an admin sees all")
    public List<RegistrationResponse> getRegistrations(@RequestParam(required = false) Long sectionId,
                                                       Authentication authentication) {
        List<Registration> registrations = registrationService.getRegistrations(authentication.getName(), sectionId);
        return registrationMapper.toResponseList(registrations);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "List the signed-in student's registrations")
    public List<RegistrationResponse> getMyRegistrations(Authentication authentication) {
        List<Registration> registrations = registrationService.getRegistrations(authentication.getName(), null);
        return registrationMapper.toResponseList(registrations);
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Register the signed-in student in a section")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request,
                                                         Authentication authentication) {
        Registration registration = registrationService.register(authentication.getName(), request.sectionId());
        return ResponseEntity
                .created(URI.create("/api/v1/registrations/" + registration.getId()))
                .body(registrationMapper.toResponse(registration));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Withdraw one of the signed-in student's registrations")
    public ResponseEntity<Void> withdraw(@PathVariable Long id, Authentication authentication) {
        registrationService.withdraw(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
