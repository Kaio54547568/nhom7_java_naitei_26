package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.email.BookingStatusEmailData;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateService emailTemplateService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(bookingRepository, emailService, emailTemplateService);
    }

    @Test
    void changeStatusShouldPersistAndSendBookingStatusEmail() {
        User user = User.builder()
                .name("Nguyen Van A")
                .email("customer@coworking.test")
                .build();
        Space space = Space.builder().name("Meeting Room A").build();
        Booking booking = Booking.builder()
                .id(42L)
                .user(user)
                .space(space)
                .startTime(LocalDateTime.of(2026, 8, 22, 9, 0))
                .endTime(LocalDateTime.of(2026, 8, 22, 11, 0))
                .totalPrice(new BigDecimal("250000.00"))
                .status("PENDING")
                .build();
        given(bookingRepository.findById(42L)).willReturn(Optional.of(booking));
        given(bookingRepository.saveAndFlush(booking)).willReturn(booking);
        given(emailTemplateService.renderBookingStatusChanged(org.mockito.ArgumentMatchers.any()))
                .willReturn("<p>Booking updated</p>");

        Booking updated = bookingService.changeStatus(42L, " approved ");

        assertThat(updated.getStatus()).isEqualTo("APPROVED");
        verify(bookingRepository).saveAndFlush(booking);
        ArgumentCaptor<BookingStatusEmailData> dataCaptor =
                ArgumentCaptor.forClass(BookingStatusEmailData.class);
        verify(emailTemplateService).renderBookingStatusChanged(dataCaptor.capture());
        assertThat(dataCaptor.getValue().bookingId()).isEqualTo(42L);
        assertThat(dataCaptor.getValue().previousStatus()).isEqualTo("PENDING");
        assertThat(dataCaptor.getValue().newStatus()).isEqualTo("APPROVED");
        assertThat(dataCaptor.getValue().userName()).isEqualTo("Nguyen Van A");
        verify(emailService).sendHtmlEmail(
                "customer@coworking.test",
                "Booking #42 status updated",
                "<p>Booking updated</p>");
    }

    @Test
    void changeStatusShouldNotPersistOrSendEmailWhenStatusIsUnchanged() {
        Booking booking = Booking.builder().id(42L).status("APPROVED").build();
        given(bookingRepository.findById(42L)).willReturn(Optional.of(booking));

        Booking unchanged = bookingService.changeStatus(42L, " approved ");

        assertThat(unchanged).isSameAs(booking);
        verifyNoInteractions(emailService, emailTemplateService);
        verify(bookingRepository, org.mockito.Mockito.never()).saveAndFlush(booking);
    }
}
