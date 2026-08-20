package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.constant.OtpPurpose;
import com.nhom7.coworkingspace.entity.OtpToken;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.repository.OtpTokenRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.OtpServiceImpl;
import com.nhom7.coworkingspace.util.OtpCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpCodeGenerator otpCodeGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        AppOtpProperties properties = new AppOtpProperties();
        properties.setExpirationMinutes(5);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        otpService = new OtpServiceImpl(
                userRepository,
                otpTokenRepository,
                emailService,
                otpCodeGenerator,
                passwordEncoder,
                properties,
                clock);
    }

    @Test
    void sendConfirmationOtpShouldReplaceOldTokenAndSendEmail() {
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("user@coworking.test")
                .status("INACTIVE")
                .build();
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(otpCodeGenerator.generateCode()).willReturn("123456");
        given(passwordEncoder.encode("123456")).willReturn("hashed-code");

        otpService.sendConfirmationOtp(user.getEmail());

        verify(otpTokenRepository).deleteByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION);
        ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
        verify(otpTokenRepository).saveAndFlush(tokenCaptor.capture());
        OtpToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getCodeHash()).isEqualTo("hashed-code");
        assertThat(savedToken.getPurpose()).isEqualTo(OtpPurpose.ACCOUNT_CONFIRMATION);
        assertThat(savedToken.getCreatedAt()).isEqualTo(NOW);
        assertThat(savedToken.getExpiresAt()).isEqualTo(NOW.plusSeconds(300));
        verify(emailService).sendHtmlEmail(
                user.getEmail(),
                "Confirm your account",
                "<p>Your confirmation code is <strong>123456</strong>.</p>");
    }

    @Test
    void sendConfirmationOtpShouldIgnoreBlankEmail() {
        otpService.sendConfirmationOtp(" ");

        verify(userRepository, never()).findByEmail(" ");
    }

    @Test
    void sendConfirmationOtpShouldNotRevealUnknownEmail() {
        String email = "unknown@coworking.test";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        otpService.sendConfirmationOtp(email);

        verifyNoInteractions(otpTokenRepository, emailService);
    }

    @Test
    void sendConfirmationOtpShouldIgnoreActiveUser() {
        User user = User.builder()
                .id(1L)
                .email("active@coworking.test")
                .status("ACTIVE")
                .build();
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

        otpService.sendConfirmationOtp(user.getEmail());

        verifyNoInteractions(otpTokenRepository, emailService);
    }
}
