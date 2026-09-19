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
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final LocalTime DATE_RANGE_START = LocalTime.MIDNIGHT;
    private static final LocalTime DATE_RANGE_END = LocalTime.MIDNIGHT.plusMinutes(1);

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
        return create(businessId, service, fields, customerName, customerPhone, null);
    }

    @Transactional
    public BookingEntity create(
            String businessId,
            String service,
            Map<String, Object> fields,
            String customerName,
            String customerPhone,
            String idempotencyKey) {

        if (businessId == null || businessId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business ID is required");
        if (service == null || service.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service is required");
        if (customerName == null || customerName.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required");
        if (customerPhone == null || customerPhone.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer phone is required");

        BusinessAccount business = businessAccountRepository.findByBusinessIdForUpdate(businessId);
        if (business == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent() && businessId.equals(existing.get().getBusinessId())) {
                return existing.get();
            }
            if (existing.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already belongs to another business");
            }
        }

        AvailabilityResult availability =
                availabilityService.check(businessId, service, fields, business.profile());
        if (availability.status() != AvailabilityStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    availability.status() == AvailabilityStatus.UNAVAILABLE
                            ? HttpStatus.CONFLICT
                            : HttpStatus.BAD_REQUEST,
                    availability.message());
        }

        LocalDate bookingDate = parseDate(firstValue(fields, "checkInDate", "date"));
        Integer durationNights = parsePositiveInt(fields, "durationNights");
        boolean dateRange = durationNights != null
                || hasAny(fields, "checkInDate", "checkOutDate");

        LocalTime startTime = parseTime(value(fields, "time"));
        if (dateRange && startTime == null) {
            startTime = DATE_RANGE_START;
        }
        if (bookingDate == null || startTime == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    dateRange
                            ? "A valid check-in date and duration are required"
                            : "A valid date and time are required");
        }

        LocalTime endTime = parseTime(value(fields, "endTime"));
        if (dateRange && endTime == null) {
            endTime = DATE_RANGE_END;
        }
        if (endTime == null) {
            endTime = startTime.plusHours(1);
        }
        if (!startTime.isBefore(endTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The requested end time must be after the start time");
        }

        LocalDate checkOutDate = durationNights == null ? null : bookingDate.plusDays(durationNights);
        if (checkOutDate == null) {
            String checkOutValue = value(fields, "checkOutDate");
            checkOutDate = parseDate(checkOutValue);
            if (checkOutDate != null && !bookingDate.isBefore(checkOutDate)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Check-out date must be after check-in date");
            }
        }

        BookingEntity booking = new BookingEntity(
                "booking_" + UUID.randomUUID(),
                businessId,
                service,
                bookingDate,
                checkOutDate,
                durationNights,
                startTime,
                endTime,
                customerName.trim(),
                customerPhone.trim(),
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );
        booking.setIdempotencyKey(idempotencyKey);
        return bookingRepository.save(booking);
    }



    @Transactional
    public BookingEntity createHold(
            String businessId,
            String service,
            Map<String, Object> fields,
            String customerName,
            String customerPhone,
            String idempotencyKey,
            int holdMinutes) {
        if (holdMinutes < 1 || holdMinutes > 1440) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hold duration must be between 1 and 1440 minutes");
        }
        BookingEntity booking = create(
                businessId, service, fields, customerName, customerPhone,
                idempotencyKey == null ? null : "hold:" + idempotencyKey);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.HOLD);
            booking.setHoldExpiresAt(LocalDateTime.now().plusMinutes(holdMinutes));
            booking = bookingRepository.save(booking);
        }
        return booking;
    }

    @Transactional
    public BookingEntity cancel(String bookingId, String businessId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!businessId.equals(booking.getBusinessId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) return booking;
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking has expired");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.HOLD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking cannot be cancelled from status " + booking.getStatus());
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    private boolean hasAny(Map<String, Object> fields, String... keys) {
        if (fields == null) return false;
        for (String key : keys) {
            if (value(fields, key) != null) return true;
        }
        return false;
    }

    private String firstValue(Map<String, Object> fields, String preferred, String fallback) {
        String value = value(fields, preferred);
        return value != null ? value : value(fields, fallback);
    }

    private String value(Map<String, Object> fields, String key) {
        if (fields == null) return null;
        Object value = fields.get(key);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer parsePositiveInt(Map<String, Object> fields, String key) {
        String value = value(fields, key);
        if (value == null) return null;
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
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

    private LocalTime parseTime(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase().replace(".", "");
        try {
            if (normalized.matches("\\d{1,2}:\\d{2}"))
                return LocalTime.parse(normalized, TIME_FORMAT);
            if (normalized.matches("\\d{1,2}\\s*(am|pm)"))
                return LocalTime.parse(normalized.toUpperCase(), DateTimeFormatter.ofPattern("h a"));
            if (normalized.matches("\\d{1,2}:\\d{2}\\s*(am|pm)"))
                return LocalTime.parse(normalized.toUpperCase(), DateTimeFormatter.ofPattern("h:mm a"));
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
    }
}
