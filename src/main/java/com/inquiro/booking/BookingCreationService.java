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

        AvailabilityResult.BookingPeriod period = availability.period();
        if (period == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Availability did not return a normalized booking period");
        }

        LocalDate bookingDate = period.startDate();
        LocalDate checkOutDate = period.endDate();
        Integer durationNights = checkOutDate == null
                ? null
                : Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(bookingDate, checkOutDate));
        LocalTime startTime = period.startTime();
        LocalTime endTime = period.endTime();

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

    

}
