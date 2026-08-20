package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.config.AppMailProperties;
import com.nhom7.coworkingspace.exception.EmailSendingException;
import com.nhom7.coworkingspace.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final AppMailProperties mailProperties;

    @Override
    public void sendPlainTextEmail(String recipient, String subject, String content) {
        sendEmail(recipient, subject, content, false);
    }

    @Override
    public void sendHtmlEmail(String recipient, String subject, String htmlContent) {
        sendEmail(recipient, subject, htmlContent, true);
    }

    private void sendEmail(String recipient, String subject, String content, boolean html) {
        // Kiem tra du lieu truoc khi gui, vi du reciepent ="" thi khong gui
        validateEmail(recipient, subject, content);

        try {
            // Tao mime message
            MimeMessage message = mailSender.createMimeMessage();
            // Tao mime helper
            // 3 tham so o day la: message - ta vua tao, false - khong tao email multipart (attachment, inline image...), UTF-8 ho tro unicode - tieng Viet
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name());
            // Gan thong tin cho email 
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(content, html);

            mailSender.send(message);
            log.info("[Email] Sent email successfully to: {}", recipient);
        } catch (MessagingException | MailException exception) {
            log.error("[Email] Failed to send email to {}: {}",
                    recipient, exception.getMessage(), exception);
            throw new EmailSendingException("Failed to send email", exception);
        }
    }

    private void validateEmail(String recipient, String subject, String content) {
        if (!StringUtils.hasText(mailProperties.getFrom())) {
            throw new IllegalStateException("Email sender address is not configured");
        }
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Email recipient must not be blank");
        }
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Email subject must not be blank");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Email content must not be blank");
        }
    }
}
