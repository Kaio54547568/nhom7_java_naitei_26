package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.constant.OtpPurpose;
import com.nhom7.coworkingspace.entity.OtpToken;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.repository.OtpTokenRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.EmailService;
import com.nhom7.coworkingspace.service.OtpService;
import com.nhom7.coworkingspace.util.OtpCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final String INACTIVE_STATUS = "INACTIVE";
    private static final String CONFIRMATION_SUBJECT = "Confirm your account";
    private static final String CONFIRMATION_BODY =
            "<p>Your confirmation code is <strong>%s</strong>.</p>";

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final OtpCodeGenerator otpCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AppOtpProperties otpProperties;
    private final Clock clock;

    @Override
    @Transactional
    public void sendConfirmationOtp(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !INACTIVE_STATUS.equals(user.getStatus())) {
            return;
        }

        OtpPurpose purpose = OtpPurpose.ACCOUNT_CONFIRMATION;
        otpTokenRepository.deleteByUserAndPurpose(user, purpose);

        String code = otpCodeGenerator.generateCode();
        Instant now = clock.instant();
        OtpToken token = OtpToken.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(code))
                .purpose(purpose)
                .createdAt(now)
                .expiresAt(now.plusSeconds(otpProperties.getExpirationMinutes() * 60))
                .build();
        otpTokenRepository.saveAndFlush(token);

        emailService.sendHtmlEmail(
                user.getEmail(),
                CONFIRMATION_SUBJECT,
                CONFIRMATION_BODY.formatted(code));
    }
}
