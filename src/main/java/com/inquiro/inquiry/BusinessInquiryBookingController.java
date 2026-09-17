package com.inquiro.inquiry;

import com.inquiro.auth.TenantAuthorizationService;
import com.inquiro.booking.BookingController.BookingResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business/accounts")
@RequiredArgsConstructor
public class BusinessInquiryBookingController {

    private final TenantAuthorizationService tenantAuthorization;
    private final BusinessInquiryBookingService service;

    @PostMapping("/{businessId}/booking-inquiry")
    public BookingInquiryResponse process(
            @PathVariable String businessId,
            @Valid @RequestBody BookingInquiryRequest request) {

        validateBusinessId(businessId);
        tenantAuthorization.requireBusinessAccess(businessId);

        BusinessInquiryBookingService.Result result = service.process(
                businessId,
                request.message(),
                request.customerName(),
                request.customerPhone()
        );

        BookingResponse booking = result.booking() == null
                ? null
                : BookingResponse.from(result.booking());

        return new BookingInquiryResponse(
                result.created(),
                result.reply(),
                result.missingFields(),
                booking
        );
    }

    private static void validateBusinessId(String businessId) {
        if (businessId == null
                || !businessId.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}")) {
            throw new IllegalArgumentException("Invalid business ID");
        }
    }

    public record BookingInquiryRequest(
            @NotBlank String message,
            @NotBlank @Size(max = 200) String customerName,
            @NotBlank @Size(max = 50) String customerPhone) {
    }

    public record BookingInquiryResponse(
            boolean bookingCreated,
            String reply,
            java.util.List<String> missingFields,
            BookingResponse booking) {
    }
}
