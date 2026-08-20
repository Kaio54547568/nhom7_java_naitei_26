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
    private EmailTemplateService emailTemplateService;

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
                emailTemplateService,
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
        given(emailTemplateService.renderAccountConfirmation("123456"))
                .willReturn("<p>Confirmation template: 123456</p>");

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
                "<p>Confirmation template: 123456</p>");
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

    @Test
    void sendPasswordResetOtpShouldReplaceOldTokenAndSendEmailForActiveUser() {
        User user = User.builder()
                .id(2L)
                .name("Active User")
                .email("active@coworking.test")
                .status("ACTIVE")
                .build();
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(otpCodeGenerator.generateCode()).willReturn("654321");
        given(passwordEncoder.encode("654321")).willReturn("hashed-reset-code");
        given(emailTemplateService.renderPasswordReset("654321"))
                .willReturn("<p>Reset template: 654321</p>");

        otpService.sendPasswordResetOtp(user.getEmail());

        verify(otpTokenRepository).deleteByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET);
        ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
        verify(otpTokenRepository).saveAndFlush(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);
        assertThat(tokenCaptor.getValue().getCodeHash()).isEqualTo("hashed-reset-code");
        verify(emailService).sendHtmlEmail(
                user.getEmail(),
                "Reset your password",
                "<p>Reset template: 654321</p>");
    }

    @Test
    void sendPasswordResetOtpShouldNormalizeEmailBeforeLookup() {
        User user = User.builder()
                .id(3L)
                .email("active@coworking.test")
                .status("ACTIVE")
                .build();
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(otpCodeGenerator.generateCode()).willReturn("111222");
        given(passwordEncoder.encode("111222")).willReturn("hashed-code");
        given(emailTemplateService.renderPasswordReset("111222")).willReturn("reset-body");

        otpService.sendPasswordResetOtp(" Active@Coworking.Test ");

        verify(userRepository).findByEmail("active@coworking.test");
        verify(emailService).sendHtmlEmail(user.getEmail(), "Reset your password", "reset-body");
    }

    @Test
    void sendPasswordResetOtpShouldNotRevealUnknownEmail() {
        String email = "unknown@coworking.test";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        otpService.sendPasswordResetOtp(email);

        verifyNoInteractions(otpTokenRepository, emailTemplateService, emailService);
    }
}
