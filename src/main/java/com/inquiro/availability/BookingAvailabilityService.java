package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.booking.BookingEntity;
import com.inquiro.booking.BookingJpaRepository;
import com.inquiro.booking.BookingStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class BookingAvailabilityService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm");

    private static final List<BookingStatus> BLOCKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingJpaRepository bookingRepository;
    private final BusinessScheduleAvailabilitySource scheduleSource;

    public BookingAvailabilityService(
            BookingJpaRepository bookingRepository,
            BusinessScheduleAvailabilitySource scheduleSource) {
        this.bookingRepository = bookingRepository;
        this.scheduleSource = scheduleSource;
    }

    /**
     * Checks both business operating hours and existing bookings.
     * The businessId is supplied explicitly so tenant identity is not
     * inferred from customer-controlled inquiry fields.
     */
    public AvailabilityResult check(
            String businessId,
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile) {

        if (businessId == null || businessId.isBlank()) {
            return unknown("A business is required to check availability.");
        }

        if (businessProfile == null) {
            return unknown("Business information is not available.");
        }

        AvailabilityResult scheduleResult =
                scheduleSource.check(service, fields, businessProfile);

        if (scheduleResult.status() != AvailabilityStatus.CONFIRMED) {
            return scheduleResult;
        }

        LocalDate date = parseDate(value(fields, "date"));
        LocalTime startTime = parseTime(value(fields, "time"));

        if (date == null || startTime == null) {
            return unknown("A valid date and time are required to check booking availability.");
        }

        LocalTime endTime = parseTime(value(fields, "endTime"));
        if (endTime == null) {
            endTime = startTime.plusHours(1);
        }

        if (!startTime.isBefore(endTime)) {
            return unknown("The requested end time must be after the start time.");
        }

        List<BookingEntity> conflicts =
                bookingRepository
                        .findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                                businessId,
                                date,
                                BLOCKING_STATUSES,
                                endTime,
                                startTime
                        );

        if (!conflicts.isEmpty()) {
            return new AvailabilityResult(
                    AvailabilityStatus.UNAVAILABLE,
                    "The requested time overlaps with an existing booking."
            );
        }

        return new AvailabilityResult(
                AvailabilityStatus.CONFIRMED,
                "The requested time is available for booking."
        );
    }

    private String value(Map<String, Object> fields, String key) {
        if (fields == null) {
            return null;
        }
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            return switch (value.toLowerCase()) {
                case "today" -> LocalDate.now();
                case "tomorrow" -> LocalDate.now().plusDays(1);
                case "day after tomorrow" -> LocalDate.now().plusDays(2);
                default -> null;
            };
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value
                .trim()
                .toLowerCase()
                .replace(".", "");

        try {
            if (normalized.matches("\\d{1,2}:\\d{2}")) {
                return LocalTime.parse(normalized, TIME_FORMAT);
            }

            if (normalized.matches("\\d{1,2}\\s*(am|pm)")) {
                return LocalTime.parse(
                        normalized.toUpperCase(),
                        DateTimeFormatter.ofPattern("h a")
                );
            }

            if (normalized.matches("\\d{1,2}:\\d{2}\\s*(am|pm)")) {
                return LocalTime.parse(
                        normalized.toUpperCase(),
                        DateTimeFormatter.ofPattern("h:mm a")
                );
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }

        return null;
    }

    private AvailabilityResult unknown(String message) {
        return new AvailabilityResult(AvailabilityStatus.UNKNOWN, message);
    }
}
