package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "space"})
    Optional<Booking> findById(Long id);
}
