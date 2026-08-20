package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.email.BookingStatusEmailData;

public interface EmailTemplateService {

    String renderAccountConfirmation(String code);

    String renderPasswordReset(String code);

    String renderBookingStatusChanged(BookingStatusEmailData data);
}
