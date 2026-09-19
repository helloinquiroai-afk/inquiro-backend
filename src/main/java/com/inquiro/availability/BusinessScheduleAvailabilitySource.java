package com.inquiro.availability;

import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

@Component
public class BusinessScheduleAvailabilitySource implements AvailabilitySource {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    @Override
    public AvailabilityResult check(
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile) {

        if (businessProfile == null || businessProfile.knowledge() == null) {
            return unknown("Business schedule information is not configured.");
        }

        BusinessKnowledge knowledge = businessProfile.knowledge();
        Map<String, String> operatingHours = knowledge.operatingHours();
        if (operatingHours == null || operatingHours.isEmpty()) {
            return unknown("Business operating hours are not configured.");
        }

        Object dateValue = fields == null ? null : fields.get("date");
        if (dateValue == null && fields != null) {
            dateValue = fields.get("checkInDate");
        }
        Object timeValue = fields == null ? null : fields.get("time");

        if (dateValue == null) {
            return unknown("A date is required to check availability.");
        }

        LocalDate date = parseDate(String.valueOf(dateValue));
        if (date == null) {
            return unknown("The requested date could not be understood.");
        }

        DayOfWeek day = date.getDayOfWeek();
        String hours = findHours(operatingHours, day);
        if (hours == null || hours.isBlank() || isClosed(hours)) {
            return new AvailabilityResult(
                    AvailabilityStatus.UNAVAILABLE,
                    "The business is closed on " + displayDay(day) + "."
            );
        }

        // Date-range services such as hotel stays only need the business
        // to be open on the requested check-in day. They do not require
        // an appointment time.
        if (timeValue == null) {
            return new AvailabilityResult(
                    AvailabilityStatus.CONFIRMED,
                    serviceDescription(service, businessProfile)
                            + " is available for the requested date."
            );
        }

        LocalTime time = parseTime(String.valueOf(timeValue));
        if (time == null) {
            return unknown("The requested time could not be understood.");
        }

        TimeRange range = parseRange(hours);
        if (range == null) {
            return unknown("The business hours could not be interpreted.");
        }

        if (range.contains(time)) {
            return new AvailabilityResult(
                    AvailabilityStatus.CONFIRMED,
                    serviceDescription(service, businessProfile)
                            + " is available on " + date + " at " + time
                            + " during business hours."
            );
        }

        return new AvailabilityResult(
                AvailabilityStatus.UNAVAILABLE,
                serviceDescription(service, businessProfile)
                        + " is not available at " + time + " on " + date
                        + " because it is outside business hours."
        );
    }

    private String findHours(Map<String, String> operatingHours, DayOfWeek day) {
        String fullName = day.name();
        String shortName = fullName.substring(0, 3);
        for (Map.Entry<String, String> entry : operatingHours.entrySet()) {
            if (normalizeDay(entry.getKey()).equals(normalizeDay(fullName))
                    || normalizeDay(entry.getKey()).equals(normalizeDay(shortName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeDay(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isClosed(String hours) {
        String normalized = hours.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("closed") || normalized.equals("close") || normalized.equals("off");
    }

    private TimeRange parseRange(String hours) {
        String normalized = hours.trim().replace("–", "-").replace("—", "-");
        String[] parts = normalized.split("-");
        if (parts.length != 2) return null;
        LocalTime opening = parseTime(parts[0].trim());
        LocalTime closing = parseTime(parts[1].trim());
        if (opening == null || closing == null) return null;
        return new TimeRange(opening, closing);
    }

    private LocalDate parseDate(String value) {
        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ignored) {
            String lower = normalized.toLowerCase(Locale.ROOT);
            LocalDate today = LocalDate.now();
            return switch (lower) {
                case "today" -> today;
                case "tomorrow" -> today.plusDays(1);
                case "day after tomorrow" -> today.plusDays(2);
                default -> null;
            };
        }
    }

    private LocalTime parseTime(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(".", "");
        try {
            if (normalized.matches("\\d{1,2}:\\d{2}")) {
                return LocalTime.parse(normalized, TIME_FORMAT);
            }
            if (normalized.matches("\\d{1,2}\\s*(am|pm)")) {
                return LocalTime.parse(
                        normalized.toUpperCase(Locale.ROOT),
                        DateTimeFormatter.ofPattern("h a", Locale.ENGLISH));
            }
            if (normalized.matches("\\d{1,2}:\\d{2}\\s*(am|pm)")) {
                return LocalTime.parse(
                        normalized.toUpperCase(Locale.ROOT),
                        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
    }

    private String displayDay(DayOfWeek day) {
        String value = day.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String serviceDescription(String service, BusinessProfile profile) {
        if (service == null || service.isBlank()) return "The requested service";
        return profile.services().stream()
                .filter(definition -> definition.requestType().equalsIgnoreCase(service))
                .map(definition -> definition.description())
                .filter(description -> description != null && !description.isBlank())
                .findFirst()
                .orElse(service);
    }

    private AvailabilityResult unknown(String message) {
        return new AvailabilityResult(AvailabilityStatus.UNKNOWN, message);
    }

    private record TimeRange(LocalTime opening, LocalTime closing) {
        boolean contains(LocalTime time) {
            return !time.isBefore(opening) && !time.isAfter(closing);
        }
    }
}
