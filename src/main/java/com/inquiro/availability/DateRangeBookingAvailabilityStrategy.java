package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.booking.BookingEntity;
import com.inquiro.booking.BookingJpaRepository;
import com.inquiro.booking.BookingStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.inquiro.booking.BookingInventoryService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
public class DateRangeBookingAvailabilityStrategy extends AbstractBookingAvailabilityStrategy {

    private static final List<BookingStatus> BLOCKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    @Autowired
    public DateRangeBookingAvailabilityStrategy(
            BookingJpaRepository bookingRepository,
            BusinessScheduleAvailabilitySource scheduleSource,
            BookingInventoryService inventoryService) {
        super(bookingRepository, scheduleSource, inventoryService);
    }

    public DateRangeBookingAvailabilityStrategy(
            BookingJpaRepository bookingRepository,
            BusinessScheduleAvailabilitySource scheduleSource) {
        super(bookingRepository, scheduleSource);
    }

    @Override
    public String id() {
        return "DATE_RANGE";
    }

    @Override
    public AvailabilityResult check(
            String businessId,
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile) {

        AvailabilityResult schedule = checkSchedule(service, fields, businessProfile);
        if (schedule.status() != AvailabilityStatus.CONFIRMED) {
            return schedule;
        }

        LocalDate checkIn = parseDate(firstValue(fields, "checkInDate", "date"));
        LocalTime startTime = parseTime(value(fields, "time"));
        Integer durationNights = positiveInt(fields, "durationNights");

        if (checkIn == null || startTime == null || durationNights == null) {
            return unknown("A valid check-in date, time, and duration are required to check booking availability.");
        }

        LocalDate checkOut = checkIn.plusDays(durationNights);
        int capacity = capacityFor(businessId, service);
        if (capacity < 1) return unknown("No booking inventory is configured for this service.");

        List<BookingEntity> bookings =
                bookingRepository.findByBusinessIdAndStatusIn(businessId, BLOCKING_STATUSES);

        long conflicts = bookings.stream().filter(booking -> overlaps(checkIn, checkOut, booking)).count();
        if (conflicts >= capacity) {
            return new AvailabilityResult(
                    AvailabilityStatus.UNAVAILABLE,
                    "The requested stay has no remaining inventory."
            );
        }

        return new AvailabilityResult(
                AvailabilityStatus.CONFIRMED,
                "The requested booking period is available."
        );
    }

    private boolean overlaps(LocalDate requestedCheckIn, LocalDate requestedCheckOut, BookingEntity existing) {
        LocalDate existingCheckIn = existing.getBookingDate();
        LocalDate existingCheckOut = existing.getCheckOutDate();
        if (existingCheckOut == null) {
            existingCheckOut = existingCheckIn.plusDays(1);
        }
        return requestedCheckIn.isBefore(existingCheckOut)
                && existingCheckIn.isBefore(requestedCheckOut);
    }
}
