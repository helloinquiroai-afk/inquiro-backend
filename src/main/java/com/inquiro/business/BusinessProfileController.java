package com.inquiro.business;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business/accounts")
@RequiredArgsConstructor
public class BusinessProfileController {

    private final BusinessAccountRepository businessAccountRepository;

    /*
     * =========================================================
     * GET BUSINESS PROFILE
     * =========================================================
     */

    @GetMapping("/{businessId}/profile")
    public BusinessProfile getBusinessProfile(
            @PathVariable String businessId) {

        BusinessAccount account =
                businessAccountRepository.findByBusinessId(
                        businessId
                );

        if (account == null) {

            throw new IllegalArgumentException(
                    "Business not found: " + businessId
            );
        }

        return account.profile();
    }


    /*
     * =========================================================
     * UPDATE BUSINESS PROFILE
     * =========================================================
     */

    @PutMapping("/{businessId}/profile")
    @org.springframework.transaction.annotation.Transactional
    public BusinessProfile updateBusinessProfile(
            @PathVariable String businessId,
            @RequestBody BusinessProfile profile) {

        if (profile == null) {

            throw new IllegalArgumentException(
                    "Business profile cannot be null"
            );
        }

        BusinessAccount account =
                businessAccountRepository.findByBusinessIdForUpdate(
                        businessId
                );

        if (account == null) {

            throw new IllegalArgumentException(
                    "Business not found: " + businessId
            );
        }

        BusinessAccount updatedAccount =
                new BusinessAccount(
                        account.businessId(),
                        account.businessName(),
                        profile
                );

        businessAccountRepository.save(
                updatedAccount
        );

        return updatedAccount.profile();
    }
}
