package com.nhom7.coworkingspace.dto.email;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingStatusEmailData(
        Long bookingId,
        String userName,
        String spaceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal totalPrice,
        String previousStatus,
        String newStatus) {
}
