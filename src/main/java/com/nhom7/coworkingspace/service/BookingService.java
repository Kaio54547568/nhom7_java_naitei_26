package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.entity.Booking;

public interface BookingService {

    Booking changeStatus(Long bookingId, String newStatus);
}
