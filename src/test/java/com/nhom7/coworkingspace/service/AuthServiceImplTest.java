package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.SignUpRequest;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.exception.EmailAlreadyExistsException;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, roleRepository, passwordEncoder, otpService);
    }

    @Test
    void signUpShouldCreateInactiveUserAndSendConfirmationOtp() {
        SignUpRequest request = new SignUpRequest(
                "Test User",
                " User@Coworking.Test ",
                "password123",
                "0901234567");
        Role userRole = Role.builder().id(1L).name("USER").build();
        given(userRepository.existsByEmail("user@coworking.test")).willReturn(false);
        given(roleRepository.findByName("USER")).willReturn(Optional.of(userRole));
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");

        authService.signUp(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo("Test User");
        assertThat(savedUser.getEmail()).isEqualTo("user@coworking.test");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getPhone()).isEqualTo("0901234567");
        assertThat(savedUser.getStatus()).isEqualTo("INACTIVE");
        assertThat(savedUser.getIsIdentityVerified()).isFalse();
        assertThat(savedUser.getIsBusinessVerified()).isFalse();
        assertThat(savedUser.getRoles()).containsExactly(userRole);
        verify(otpService).sendConfirmationOtp("user@coworking.test");
    }

    @Test
    void signUpShouldRejectExistingEmail() {
        SignUpRequest request = new SignUpRequest(
                "Existing User",
                "existing@coworking.test",
                "password123",
                null);
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verifyNoInteractions(roleRepository, passwordEncoder, otpService);
    }
}
