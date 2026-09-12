package com.inquiro.business;

import com.inquiro.request.RequestDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/business/accounts")
@RequiredArgsConstructor
public class BusinessAccountController {

    private final BusinessAccountRepository businessAccountRepository;

    /**
     * Create a new business account with its initial onboarding information.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessAccountResponse createBusinessAccount(
            @Valid @RequestBody CreateBusinessAccountRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        String businessId = "biz_" + UUID.randomUUID();

        BusinessKnowledge knowledge =
                request.knowledge() == null
                        ? BusinessKnowledge.empty()
                        : request.knowledge();

        List<RequestDefinition> services =
                request.services() == null
                        ? List.of()
                        : request.services();

        BusinessProfile profile =
                new BusinessProfile(
                        request.businessName(),
                        request.businessType(),
                        request.description(),
                        services,
                        knowledge
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        request.businessName(),
                        profile
                );

        businessAccountRepository.save(account);

        return toResponse(account);
    }

    /**
     * Get the complete onboarding state for a business.
     */
    @GetMapping("/{businessId}/onboarding")
    public OnboardingResponse getOnboarding(
            @PathVariable String businessId) {

        validateBusinessId(businessId);

        BusinessAccount account =
                businessAccountRepository.findByBusinessId(businessId);

        if (account == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Business not found"
            );
        }

        return toOnboardingResponse(account);
    }

    /**
     * Replace the onboarding configuration.
     *
     * Business identity, services and knowledge are replaced together.
     *
     * The transaction is required because the persistence layer uses
     * a PESSIMISTIC_WRITE lock when loading the account for update.
     */
    @Transactional
    @PutMapping("/{businessId}/onboarding")
    public OnboardingResponse updateOnboarding(
            @PathVariable String businessId,
            @Valid @RequestBody UpdateOnboardingRequest request) {

        validateBusinessId(businessId);

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        BusinessAccount existing =
                businessAccountRepository.findByBusinessIdForUpdate(
                        businessId
                );

        if (existing == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Business not found"
            );
        }

        BusinessProfile profile =
                new BusinessProfile(
                        request.businessName(),
                        request.businessType(),
                        request.description(),
                        request.services() == null
                                ? List.of()
                                : request.services(),
                        request.knowledge() == null
                                ? BusinessKnowledge.empty()
                                : request.knowledge()
                );

        BusinessAccount updated =
                new BusinessAccount(
                        existing.businessId(),
                        request.businessName(),
                        profile
                );

        businessAccountRepository.save(updated);

        return toOnboardingResponse(updated);
    }

    private BusinessAccountResponse toResponse(
            BusinessAccount account) {

        return new BusinessAccountResponse(
                account.businessId(),
                account.businessName()
        );
    }

    private OnboardingResponse toOnboardingResponse(
            BusinessAccount account) {

        BusinessProfile profile = account.profile();

        return new OnboardingResponse(
                account.businessId(),
                profile.businessName(),
                profile.businessType(),
                profile.description(),
                profile.services(),
                profile.knowledge()
        );
    }

    private static void validateBusinessId(String businessId) {

        if (businessId == null
                || !businessId.matches(
                "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}")) {

            throw new IllegalArgumentException(
                    "Invalid business ID"
            );
        }
    }

    /**
     * Request used when creating a business.
     */
    public record CreateBusinessAccountRequest(

            @NotBlank
            @Size(max = 200)
            String businessName,

            @Size(max = 100)
            String businessType,

            @Size(max = 5000)
            String description,

            List<@Valid RequestDefinition> services,

            @Valid
            BusinessKnowledge knowledge

    ) {
    }

    /**
     * Request used when updating the onboarding configuration.
     */
    public record UpdateOnboardingRequest(

            @NotBlank
            @Size(max = 200)
            String businessName,

            @Size(max = 100)
            String businessType,

            @Size(max = 5000)
            String description,

            List<@Valid RequestDefinition> services,

            @Valid
            BusinessKnowledge knowledge

    ) {
    }

    /**
     * Response returned after business creation.
     */
    public record BusinessAccountResponse(
            String businessId,
            String businessName
    ) {
    }

    /**
     * Complete onboarding state.
     */
    public record OnboardingResponse(
            String businessId,
            String businessName,
            String businessType,
            String description,
            List<RequestDefinition> services,
            BusinessKnowledge knowledge
    ) {
    }
}