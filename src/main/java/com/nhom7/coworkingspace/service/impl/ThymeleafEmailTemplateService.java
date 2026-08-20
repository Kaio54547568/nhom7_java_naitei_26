package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.dto.email.BookingStatusEmailData;
import com.nhom7.coworkingspace.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class ThymeleafEmailTemplateService implements EmailTemplateService {

    private static final String ACCOUNT_CONFIRMATION_TEMPLATE = "email/account-confirmation";
    private static final String PASSWORD_RESET_TEMPLATE = "email/password-reset";
    private static final String BOOKING_STATUS_CHANGED_TEMPLATE = "email/booking-status-changed";

    private final TemplateEngine templateEngine;
    private final AppOtpProperties otpProperties;

    @Override
    public String renderAccountConfirmation(String code) {
        return render(ACCOUNT_CONFIRMATION_TEMPLATE, code);
    }

    @Override
    public String renderPasswordReset(String code) {
        return render(PASSWORD_RESET_TEMPLATE, code);
    }

    @Override
    public String renderBookingStatusChanged(BookingStatusEmailData data) {
        Context context = new Context();
        context.setVariable("booking", data);
        return templateEngine.process(BOOKING_STATUS_CHANGED_TEMPLATE, context);
    }

    private String render(String template, String code) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expirationMinutes", otpProperties.getExpirationMinutes());
        return templateEngine.process(template, context);
    }
}
