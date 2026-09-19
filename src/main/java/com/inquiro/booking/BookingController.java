package com.inquiro.booking;

import com.inquiro.auth.TenantAuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/business/accounts")
@RequiredArgsConstructor
public class BookingController {

    private final TenantAuthorizationService tenantAuthorization;
    private final BookingCreationService bookingCreationService;

    @PostMapping("/{businessId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @PathVariable String businessId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {

        validateBusinessId(businessId);
        tenantAuthorization.requireBusinessAccess(businessId);

        BookingEntity booking = bookingCreationService.create(
                businessId,
                request.service(),
                request.fields(),
                request.customerName(),
                request.customerPhone(),
                normalizeIdempotencyKey(idempotencyKey)
        );

        return BookingResponse.from(booking);
    }


    @PostMapping("/{businessId}/booking-holds")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createHold(
            @PathVariable String businessId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateHoldRequest request) {
        validateBusinessId(businessId);
        tenantAuthorization.requireBusinessWriteAccess(businessId);
        BookingEntity booking = bookingCreationService.createHold(
                businessId, request.service(), request.fields(),
                request.customerName(), request.customerPhone(),
                normalizeIdempotencyKey(idempotencyKey), request.holdMinutes());
        return BookingResponse.from(booking);
    }

    @PostMapping("/{businessId}/bookings/{bookingId}/cancel")
    public BookingResponse cancelBooking(
            @PathVariable String businessId,
            @PathVariable String bookingId) {
        validateBusinessId(businessId);
        tenantAuthorization.requireBusinessWriteAccess(businessId);
        BookingEntity booking = bookingCreationService.cancel(bookingId, businessId);
        return BookingResponse.from(booking);
    }

    private static String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("Idempotency-Key is too long");
        }
        return normalized;
    }

    private static void validateBusinessId(String businessId) {
        if (businessId == null || !businessId.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}")) {
            throw new IllegalArgumentException("Invalid business ID");
        }
    }

    public record CreateHoldRequest(
            @NotBlank String service,
            Map<String, Object> fields,
            @NotBlank @Size(max = 200) String customerName,
            @NotBlank @Size(max = 50) String customerPhone,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(1440) int holdMinutes
    ) {
        public CreateHoldRequest {
            fields = fields == null ? Map.of() : Map.copyOf(fields);
        }
    }

    public record CreateBookingRequest(
            @NotBlank String service,
            Map<String, Object> fields,
            @NotBlank @Size(max = 200) String customerName,
            @NotBlank @Size(max = 50) String customerPhone
    ) {
        public CreateBookingRequest {
            fields = fields == null ? Map.of() : Map.copyOf(fields);
        }
    }

    public record BookingResponse(
            String bookingId,
            String businessId,
            String service,
            LocalDate bookingDate,
            LocalDate checkOutDate,
            Integer durationNights,
            LocalTime startTime,
            LocalTime endTime,
            String customerName,
            String customerPhone,
            BookingStatus status,
            LocalDateTime createdAt
    ) {
        public static BookingResponse from(BookingEntity booking) {
            return new BookingResponse(
                    booking.getBookingId(),
                    booking.getBusinessId(),
                    booking.getService(),
                    booking.getBookingDate(),
                    booking.getCheckOutDate(),
                    booking.getDurationNights(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getCustomerName(),
                    booking.getCustomerPhone(),
                    booking.getStatus(),
                    booking.getCreatedAt()
            );
        }
    }
}
