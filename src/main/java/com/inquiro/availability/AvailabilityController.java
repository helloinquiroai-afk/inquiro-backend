package com.inquiro.availability;

import com.inquiro.auth.TenantAuthorizationService;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/business/accounts")
@RequiredArgsConstructor
public class AvailabilityController {

    private final BusinessAccountRepository businessAccountRepository;
    private final TenantAuthorizationService tenantAuthorization;
    private final BookingAvailabilityService bookingAvailabilityService;

    @PostMapping("/{businessId}/availability")
    public AvailabilityResult checkAvailability(
            @PathVariable String businessId,
            @Valid @RequestBody AvailabilityRequest request) {

        validateBusinessId(businessId);
        tenantAuthorization.requireBusinessAccess(businessId);

        BusinessAccount account =
                businessAccountRepository.findByBusinessId(businessId);

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Business not found"
            );
        }

        return bookingAvailabilityService.check(
                businessId,
                request.service(),
                request.fields(),
                account.profile()
        );
    }

    private static void validateBusinessId(String businessId) {
        if (businessId == null
                || !businessId.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}")) {
            throw new IllegalArgumentException("Invalid business ID");
        }
    }

    public record AvailabilityRequest(
            @NotBlank String service,
            Map<String, Object> fields
    ) {
        public AvailabilityRequest {
            fields = fields == null ? Map.of() : Map.copyOf(fields);
        }
    }
}
