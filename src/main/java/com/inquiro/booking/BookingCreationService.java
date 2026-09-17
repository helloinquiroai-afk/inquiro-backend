package com.inquiro.booking;

import com.inquiro.availability.AvailabilityResult;
import com.inquiro.availability.AvailabilityStatus;
import com.inquiro.availability.BookingAvailabilityService;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingCreationService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm");

    private final BusinessAccountRepository businessAccountRepository;
    private final BookingJpaRepository bookingRepository;
    private final BookingAvailabilityService availabilityService;

    @Transactional
    public BookingEntity create(
            String businessId,
            String service,
            Map<String, Object> fields,
            String customerName,
            String customerPhone) {

        if (businessId == null || businessId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business ID is required");
        }

        if (service == null || service.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service is required");
        }

        if (customerName == null || customerName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required");
        }

        if (customerPhone == null || customerPhone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer phone is required");
        }

        BusinessAccount business =
                businessAccountRepository.findByBusinessIdForUpdate(businessId);

        if (business == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }

        AvailabilityResult availability =
                availabilityService.check(
                        businessId,
                        service,
                        fields,
                        business.profile()
                );

        if (availability.status() != AvailabilityStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    availability.status() == AvailabilityStatus.UNAVAILABLE
                            ? HttpStatus.CONFLICT
                            : HttpStatus.BAD_REQUEST,
                    availability.message()
            );
        }

        LocalDate date = parseDate(value(fields, "date"));
        LocalTime startTime = parseTime(value(fields, "time"));
        LocalTime endTime = parseTime(value(fields, "endTime"));

        if (date == null || startTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid date and time are required");
        }

        if (endTime == null) {
            endTime = startTime.plusHours(1);
        }

        if (!startTime.isBefore(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The requested end time must be after the start time");
        }

        BookingEntity booking = new BookingEntity(
                "booking_" + UUID.randomUUID(),
                businessId,
                service,
                date,
                startTime,
                endTime,
                customerName.trim(),
                customerPhone.trim(),
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );

        return bookingRepository.save(booking);
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
        String normalized = value.trim().toLowerCase().replace(".", "");
        try {
            if (normalized.matches("\\d{1,2}:\\d{2}")) {
                return LocalTime.parse(normalized, TIME_FORMAT);
            }
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
}
