package com.project.acados.security;

import com.project.acados.domain.entity.User;
import com.project.acados.domain.enums.UserRole;
import com.project.acados.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .universityId("T001")
                .email("teacher@acados.ac.th")
                .passwordHash("bcryptEncodedPassword")
                .role(UserRole.TEACHER)
                .build();
    }

    @Test
    @DisplayName("loadUserByUsername: should find user by universityId")
    void testLoadUserByUsername_ByUniversityId() {
        when(userRepository.findByUniversityId("T001")).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("T001");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("T001");
        assertThat(userDetails.getPassword()).isEqualTo("bcryptEncodedPassword");
        assertThat(userDetails.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        verify(userRepository).findByUniversityId("T001");
    }

    @Test
    @DisplayName("loadUserByUsername: should find user by email when universityId not found")
    void testLoadUserByUsername_ByEmail() {
        when(userRepository.findByUniversityId("teacher@acados.ac.th")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("teacher@acados.ac.th")).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("teacher@acados.ac.th");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("T001");
        assertThat(userDetails.getPassword()).isEqualTo("bcryptEncodedPassword");
        verify(userRepository).findByUniversityId("teacher@acados.ac.th");
        verify(userRepository).findByEmail("teacher@acados.ac.th");
    }

    @Test
    @DisplayName("loadUserByUsername: should throw UsernameNotFoundException when neither universityId nor email matches")
    void testLoadUserByUsername_NotFound() {
        when(userRepository.findByUniversityId("UNKNOWN")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("UNKNOWN"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with identifier: UNKNOWN");

        verify(userRepository).findByUniversityId("UNKNOWN");
        verify(userRepository).findByEmail("UNKNOWN");
    }
}

