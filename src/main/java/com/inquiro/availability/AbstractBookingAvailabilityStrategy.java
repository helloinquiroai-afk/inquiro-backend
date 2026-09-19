package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.booking.BookingJpaRepository;
import com.inquiro.booking.BookingInventoryService;
import com.inquiro.request.RequestDefinition;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractBookingAvailabilityStrategy implements BookingAvailabilityStrategy {

    protected static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    protected final BookingJpaRepository bookingRepository;
    protected final BusinessScheduleAvailabilitySource scheduleSource;
    protected final BookingInventoryService inventoryService;

    protected AbstractBookingAvailabilityStrategy(BookingJpaRepository bookingRepository, BusinessScheduleAvailabilitySource scheduleSource) {
        this(bookingRepository, scheduleSource, null);
    }

    protected AbstractBookingAvailabilityStrategy(BookingJpaRepository bookingRepository, BusinessScheduleAvailabilitySource scheduleSource, BookingInventoryService inventoryService) {
        this.bookingRepository = bookingRepository;
        this.scheduleSource = scheduleSource;
        this.inventoryService = inventoryService;
    }

    protected int capacityFor(String businessId, String service) {
        return inventoryService == null ? 1 : inventoryService.capacityFor(businessId, service);
    }

    protected AvailabilityResult checkSchedule(String service, Map<String, Object> fields, BusinessProfile businessProfile) {
        return scheduleSource.check(service, fields, businessProfile);
    }

    protected Map<String, Object> mapFields(
            Map<String, Object> fields,
            RequestDefinition definition,
            Map<String, String> defaults) {
        Map<String, Object> result = new HashMap<>();
        if (fields != null) result.putAll(fields);
        Map<String, String> config = definition == null ? Map.of() : definition.availabilityFields();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String target = entry.getKey();
            String configuredSlot = config.get(target);
            String source = configuredSlot == null || configuredSlot.isBlank()
                    ? entry.getValue()
                    : configuredSlot;
            if (result.get(target) == null && source != null && result.get(source) != null) {
                result.put(target, result.get(source));
            }
        }
        if (result.get("date") == null && result.get("startDate") != null) {
            result.put("date", result.get("startDate"));
        }
        return result;
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
            if (normalized.matches("\\d{1,2}:\\d{2}")) return LocalTime.parse(normalized, TIME_FORMAT);
            if (normalized.matches("\\d{1,2}\\s*(am|pm)")) {
                return LocalTime.parse(normalized.toUpperCase(), DateTimeFormatter.ofPattern("h a"));
            }
            if (normalized.matches("\\d{1,2}:\\d{2}\\s*(am|pm)")) {
                return LocalTime.parse(normalized.toUpperCase(), DateTimeFormatter.ofPattern("h:mm a"));
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
