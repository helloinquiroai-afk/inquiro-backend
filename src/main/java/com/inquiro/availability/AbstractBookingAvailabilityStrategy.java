package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.booking.BookingJpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

abstract class AbstractBookingAvailabilityStrategy implements BookingAvailabilityStrategy {

    protected static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    protected final BookingJpaRepository bookingRepository;
    protected final BusinessScheduleAvailabilitySource scheduleSource;

    protected AbstractBookingAvailabilityStrategy(
            BookingJpaRepository bookingRepository,
            BusinessScheduleAvailabilitySource scheduleSource) {
        this.bookingRepository = bookingRepository;
        this.scheduleSource = scheduleSource;
    }

    protected AvailabilityResult checkSchedule(
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile) {

        Map<String, Object> scheduleFields = fields;
        if (fields != null && fields.get("date") == null && fields.get("checkInDate") != null) {
            scheduleFields = new java.util.HashMap<>(fields);
            scheduleFields.put("date", fields.get("checkInDate"));
        }
        return scheduleSource.check(service, scheduleFields, businessProfile);
    }

    protected LocalDate parseDate(String value) {
        if (value == null) return null;
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

    protected LocalTime parseTime(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase().replace(".", "");
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

    protected String value(Map<String, Object> fields, String key) {
        if (fields == null) return null;
        Object value = fields.get(key);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    protected String firstValue(Map<String, Object> fields, String preferred, String fallback) {
        String value = value(fields, preferred);
        return value != null ? value : value(fields, fallback);
    }

    protected Integer positiveInt(Map<String, Object> fields, String key) {
        String value = value(fields, key);
        if (value == null) return null;
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    protected AvailabilityResult unknown(String message) {
        return new AvailabilityResult(AvailabilityStatus.UNKNOWN, message);
    }
}
