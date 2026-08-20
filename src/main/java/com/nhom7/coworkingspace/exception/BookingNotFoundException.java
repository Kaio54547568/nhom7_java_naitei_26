package com.nhom7.coworkingspace.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long bookingId) {
        super("Booking not found: " + bookingId);
    }
}
