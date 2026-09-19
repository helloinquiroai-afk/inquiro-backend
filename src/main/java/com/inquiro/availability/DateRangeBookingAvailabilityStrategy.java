package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.booking.BookingEntity;
import com.inquiro.booking.BookingJpaRepository;
import com.inquiro.booking.BookingStatus;
import com.inquiro.request.RequestDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
public class DateRangeBookingAvailabilityStrategy extends AbstractBookingAvailabilityStrategy {

    private static final List<BookingStatus> BLOCKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.HOLD, BookingStatus.CONFIRMED);
    private static final LocalTime DEFAULT_START = LocalTime.MIDNIGHT;
    private static final LocalTime DEFAULT_END = LocalTime.MIDNIGHT.plusMinutes(1);

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
            BusinessProfile businessProfile,
            RequestDefinition definition) {

        Map<String, Object> mapped = mapFields(fields, definition, Map.of(
                "date", "date",
                "startDate", "checkInDate",
                "endDate", "checkOutDate",
                "duration", "durationNights",
                "startTime", "time"
        ));

        AvailabilityResult schedule = checkSchedule(service, mapped, businessProfile);
        if (schedule.status() != AvailabilityStatus.CONFIRMED) return schedule;

        LocalDate startDate = parseDate(value(mapped, "startDate"));
        if (startDate == null) startDate = parseDate(value(mapped, "date"));

        Integer duration = positiveInt(mapped, "duration");
        LocalDate endDate = parseDate(value(mapped, "endDate"));
        if (endDate == null && duration != null) endDate = startDate == null ? null : startDate.plusDays(duration);

        LocalTime startTime = parseTime(value(mapped, "startTime"));
        if (startTime == null) startTime = DEFAULT_START;
        LocalTime endTime = parseTime(value(mapped, "endTime"));
        if (endTime == null) endTime = DEFAULT_END;

        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            return unknown("A valid booking start date and end date (or duration) are required.");
        }

        int capacity = capacityFor(businessId, service);
        if (capacity < 1) return unknown("No booking inventory is configured for this service.");

        List<BookingEntity> bookings =
                bookingRepository.findByBusinessIdAndStatusIn(businessId, BLOCKING_STATUSES);

        long conflicts = bookings.stream().filter(booking -> overlaps(startDate, endDate, booking)).count();
        if (conflicts >= capacity) {
            return new AvailabilityResult(
                    AvailabilityStatus.UNAVAILABLE,
                    "The requested booking period has no remaining inventory."
            );
        }

        return new AvailabilityResult(
                AvailabilityStatus.CONFIRMED,
                "The requested booking period is available.",
                new AvailabilityResult.BookingPeriod(startDate, endDate, startTime, endTime)
        );
    }

    private boolean overlaps(LocalDate requestedStart, LocalDate requestedEnd, BookingEntity existing) {
        LocalDate existingStart = existing.getBookingDate();
        LocalDate existingEnd = existing.getCheckOutDate();
        if (existingStart == null) return false;
        if (existingEnd == null) existingEnd = existingStart.plusDays(1);
        return requestedStart.isBefore(existingEnd) && existingStart.isBefore(requestedEnd);
    }
}
