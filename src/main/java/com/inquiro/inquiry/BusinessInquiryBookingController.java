package com.inquiro.inquiry;

import com.inquiro.booking.BookingResponse;
import com.inquiro.tenant.TenantAuthorizationService;
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

    private void validateBusinessId(String businessId) {
        if (businessId == null || businessId.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Business ID is required");
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
