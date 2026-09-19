package com.inquiro.availability;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityResult(
        AvailabilityStatus status,
        String message,
        BookingPeriod period
) {
    public AvailabilityResult(AvailabilityStatus status, String message) {
        this(status, message, null);
    }

    public record BookingPeriod(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        public BookingPeriod {
            if (startDate == null) throw new IllegalArgumentException("startDate is required");
            if (startTime == null) throw new IllegalArgumentException("startTime is required");
            if (endTime == null) throw new IllegalArgumentException("endTime is required");
            if (!startTime.isBefore(endTime)) {
                throw new IllegalArgumentException("startTime must be before endTime");
            }
            if (endDate != null && !startDate.isBefore(endDate)) {
                throw new IllegalArgumentException("endDate must be after startDate");
            }
        }
    }
}
