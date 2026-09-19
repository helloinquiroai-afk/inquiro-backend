package com.inquiro.booking;

import com.inquiro.auth.TenantAuthorizationService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/business/accounts")
@RequiredArgsConstructor
public class BookingManagementController {

    private final TenantAuthorizationService tenantAuthorization;
    private final BookingManagementService bookingManagementService;

    @GetMapping("/{businessId}/bookings")
    public List<BookingResponse> listBookings(
            @PathVariable String businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        validateBusinessId(businessId);
        tenantAuthorization.requireBusinessAccess(businessId);

        return bookingManagementService.list(businessId, date)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @GetMapping("/{businessId}/bookings/{bookingId}")
    public BookingResponse getBooking(
            @PathVariable String businessId,
            @PathVariable @Size(max = 160) String bookingId) {

        validateBusinessId(businessId);
        tenantAuthorization.requireBusinessAccess(businessId);

        return BookingResponse.from(bookingManagementService.get(businessId, bookingId));
    }

    @DeleteMapping("/{businessId}/bookings/{bookingId}")
    public BookingResponse cancelBooking(
            @PathVariable String businessId,
            @PathVariable @Size(max = 160) String bookingId) {

        validateBusinessId(businessId);
        tenantAuthorization.requireBusinessAccess(businessId);

        return BookingResponse.from(bookingManagementService.cancel(businessId, bookingId));
    }

    private static void validateBusinessId(String businessId) {
        if (businessId == null || !businessId.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}")) {
            throw new IllegalArgumentException("Invalid business ID");
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
        static BookingResponse from(BookingEntity booking) {
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
