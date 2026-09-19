package com.inquiro.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingJpaRepository
        extends JpaRepository<BookingEntity, String> {

    List<BookingEntity> findByBusinessIdAndBookingDateAndStatusIn(
            String businessId,
            LocalDate bookingDate,
            List<BookingStatus> statuses
    );

    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);

    List<BookingEntity> findByBusinessIdAndStatusIn(
            String businessId,
            List<BookingStatus> statuses
    );

    List<BookingEntity> findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            String businessId,
            LocalDate bookingDate,
            List<BookingStatus> statuses,
            LocalTime requestedEndTime,
            LocalTime requestedStartTime
    );
}
