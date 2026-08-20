package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.dto.email.BookingStatusEmailData;
import com.nhom7.coworkingspace.service.impl.ThymeleafEmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafEmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        AppOtpProperties otpProperties = new AppOtpProperties();
        otpProperties.setExpirationMinutes(7);
        emailTemplateService = new ThymeleafEmailTemplateService(templateEngine, otpProperties);
    }

    @Test
    void renderAccountConfirmationShouldIncludeOtpCode() {
        String html = emailTemplateService.renderAccountConfirmation("123456");

        assertThat(html)
                .contains("Confirm your account")
                .contains("123456")
                .contains(">7</span> minutes");
    }

    @Test
    void renderPasswordResetShouldIncludeOtpCode() {
        String html = emailTemplateService.renderPasswordReset("654321");

        assertThat(html)
                .contains("Reset your password")
                .contains("654321")
                .contains(">7</span> minutes");
    }

    @Test
    void renderBookingStatusChangedShouldIncludeBookingDetails() {
        BookingStatusEmailData data = new BookingStatusEmailData(
                42L,
                "Nguyen Van A",
                "Meeting Room A",
                LocalDateTime.of(2026, 8, 22, 9, 0),
                LocalDateTime.of(2026, 8, 22, 11, 0),
                new BigDecimal("250000.00"),
                "PENDING",
                "APPROVED");

        String html = emailTemplateService.renderBookingStatusChanged(data);

        assertThat(html)
                .contains("Booking status updated")
                .contains("Nguyen Van A")
                .contains("#42")
                .contains("Meeting Room A")
                .contains("PENDING")
                .contains("APPROVED")
                .contains("250000.00");
    }
}
