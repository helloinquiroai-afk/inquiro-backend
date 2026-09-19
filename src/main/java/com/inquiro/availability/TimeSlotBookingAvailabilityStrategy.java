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
public class TimeSlotBookingAvailabilityStrategy extends AbstractBookingAvailabilityStrategy {

    private static final List<BookingStatus> BLOCKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.HOLD, BookingStatus.CONFIRMED);

    @Autowired
    public TimeSlotBookingAvailabilityStrategy(
            BookingJpaRepository bookingRepository,
            BusinessScheduleAvailabilitySource scheduleSource,
            com.inquiro.booking.BookingInventoryService inventoryService) {
        super(bookingRepository, scheduleSource, inventoryService);
    }

    public TimeSlotBookingAvailabilityStrategy(
            BookingJpaRepository bookingRepository,
            BusinessScheduleAvailabilitySource scheduleSource) {
        super(bookingRepository, scheduleSource);
    }

    @Override
    public String id() {
        return "TIME_SLOT";
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

        LocalDate date = parseDate(firstValue(fields, "date", "checkInDate"));
        LocalTime startTime = parseTime(value(fields, "time"));
        if (date == null || startTime == null) {
            return unknown("A valid date and time are required to check booking availability.");
        }

        LocalTime endTime = parseTime(value(fields, "endTime"));
        if (endTime == null) endTime = startTime.plusHours(1);
        if (!startTime.isBefore(endTime)) {
            return unknown("The requested end time must be after the start time.");
        }

        List<BookingEntity> conflicts =
                bookingRepository.findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        businessId,
                        date,
                        BLOCKING_STATUSES,
                        endTime,
                        startTime
                );

        int capacity = capacityFor(businessId, service);
        if (capacity < 1) return unknown("No booking inventory is configured for this service.");
        if (conflicts.size() >= capacity) {
            return new AvailabilityResult(
                    AvailabilityStatus.UNAVAILABLE,
                    "The requested time has no remaining inventory."
            );
        }

        return new AvailabilityResult(
                AvailabilityStatus.CONFIRMED,
                "The requested time is available for booking."
        );
    }
}
