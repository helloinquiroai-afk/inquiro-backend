package com.inquiro.business;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/business/accounts")
@RequiredArgsConstructor
public class BusinessAccountController {

    private final BusinessAccountRepository businessAccountRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessAccountResponse createBusinessAccount(
            @RequestBody CreateBusinessAccountRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        if (request.businessName() == null
                || request.businessName().isBlank()) {

            throw new IllegalArgumentException(
                    "Business name is required"
            );
        }

        /*
         * For this first onboarding API we create
         * a minimal BusinessProfile.
         *
         * Business knowledge/services will be added
         * in the next onboarding step.
         */

        BusinessProfile profile =
                new BusinessProfile(
                        request.businessName(),
                        request.businessType(),
                        request.description(),
                        java.util.List.of(),
                        null
                );

        String businessId =
                "biz_" + UUID.randomUUID();

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        request.businessName(),
                        profile
                );

        businessAccountRepository.save(
                account
        );

        return new BusinessAccountResponse(
                account.businessId(),
                account.businessName()
        );
    }


    /*
     * =========================================================
     * CREATE BUSINESS REQUEST
     * =========================================================
     */

    public record CreateBusinessAccountRequest(

            String businessName,

            String businessType,

            String description

    ) {
    }


    /*
     * =========================================================
     * CREATE BUSINESS RESPONSE
     * =========================================================
     */

    public record BusinessAccountResponse(

            String businessId,

            String businessName

    ) {
    }
}