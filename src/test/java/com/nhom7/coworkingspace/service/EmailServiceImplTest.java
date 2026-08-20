package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.config.AppMailProperties;
import com.nhom7.coworkingspace.exception.EmailSendingException;
import com.nhom7.coworkingspace.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl - Unit Tests")
class EmailServiceImplTest {

    private static final String SENDER = "no-reply@coworking.test";
    private static final String RECIPIENT = "user@coworking.test";

    @Mock
    private JavaMailSender mailSender;

    private AppMailProperties mailProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailProperties = new AppMailProperties();
        mailProperties.setFrom(SENDER);
        emailService = new EmailServiceImpl(mailSender, mailProperties);
    }

    @Test
    @DisplayName("Should send a UTF-8 plain text email")
    void givenPlainTextEmail_whenSend_thenMessageContainsExpectedData() throws Exception {
        MimeMessage message = createMimeMessage();
        given(mailSender.createMimeMessage()).willReturn(message);

        emailService.sendPlainTextEmail(RECIPIENT, "Welcome", "Hello from Coworking Space");
        message.saveChanges();

        verify(mailSender).send(message);
        assertThat(message.getFrom()).containsExactly(new InternetAddress(SENDER));
        assertThat(message.getAllRecipients()).containsExactly(new InternetAddress(RECIPIENT));
        assertThat(message.getSubject()).isEqualTo("Welcome");
        assertThat(message.getContentType()).startsWith("text/plain");
        assertThat(message.getContent()).isEqualTo("Hello from Coworking Space");
    }

    @Test
    @DisplayName("Should send an HTML email")
    void givenHtmlEmail_whenSend_thenMessageUsesHtmlContentType() throws Exception {
        MimeMessage message = createMimeMessage();
        given(mailSender.createMimeMessage()).willReturn(message);

        emailService.sendHtmlEmail(RECIPIENT, "Confirm account", "<strong>123456</strong>");
        message.saveChanges();

        verify(mailSender).send(message);
        assertThat(message.getContentType()).startsWith("text/html");
        assertThat(message.getContent()).isEqualTo("<strong>123456</strong>");
    }

    @Test
    @DisplayName("Should reject a blank recipient before creating a message")
    void givenBlankRecipient_whenSend_thenThrowIllegalArgumentException() {
        assertThatThrownBy(() -> emailService.sendPlainTextEmail(" ", "Subject", "Content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email recipient must not be blank");

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("Should reject sending when sender address is not configured")
    void givenMissingSender_whenSend_thenThrowIllegalStateException() {
        mailProperties.setFrom(" ");

        assertThatThrownBy(() -> emailService.sendPlainTextEmail(RECIPIENT, "Subject", "Content"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email sender address is not configured");

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("Should wrap mail transport errors in EmailSendingException")
    void givenMailTransportError_whenSend_thenThrowEmailSendingException() {
        MimeMessage message = createMimeMessage();
        given(mailSender.createMimeMessage()).willReturn(message);
        willThrow(new MailSendException("SMTP unavailable")).given(mailSender).send(message);

        assertThatThrownBy(() -> emailService.sendPlainTextEmail(
                RECIPIENT, "Subject", "Content"))
                .isInstanceOf(EmailSendingException.class)
                .hasMessage("Failed to send email")
                .hasCauseInstanceOf(MailSendException.class);
    }

    private MimeMessage createMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
