package com.inquiro.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingJpaRepository
        extends JpaRepository<BookingEntity, String> {

    List<BookingEntity> findByBusinessIdAndBookingDateAndStatusIn(
            String businessId,
            LocalDate bookingDate,
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
