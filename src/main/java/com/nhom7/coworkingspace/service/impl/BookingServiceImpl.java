package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.email.BookingStatusEmailData;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.exception.BookingNotFoundException;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.service.BookingService;
import com.nhom7.coworkingspace.service.EmailService;
import com.nhom7.coworkingspace.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Override
    @Transactional
    public Booking changeStatus(Long bookingId, String newStatus) {
        String normalizedStatus = normalizeStatus(newStatus);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        String previousStatus = booking.getStatus();

        if (normalizedStatus.equalsIgnoreCase(previousStatus)) {
            return booking;
        }

        booking.setStatus(normalizedStatus);
        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        BookingStatusEmailData emailData = new BookingStatusEmailData(
                savedBooking.getId(),
                savedBooking.getUser().getName(),
                savedBooking.getSpace().getName(),
                savedBooking.getStartTime(),
                savedBooking.getEndTime(),
                savedBooking.getTotalPrice(),
                previousStatus,
                normalizedStatus);
        String html = emailTemplateService.renderBookingStatusChanged(emailData);
        emailService.sendHtmlEmail(
                savedBooking.getUser().getEmail(),
                "Booking #" + savedBooking.getId() + " status updated",
                html);
        return savedBooking;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Booking status must not be blank");
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }
}
