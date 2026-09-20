package com.inquiro.business.onboarding;

import com.inquiro.auth.TenantAuthorizationService;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business/accounts/{businessId}/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final BusinessAccountRepository businessAccountRepository;
    private final TenantAuthorizationService tenantAuthorization;

    @GetMapping("/catalog")
    public OnboardingCatalogResponse catalog(@PathVariable String businessId) {
        requireAccess(businessId);
        BusinessAccount account = requireAccount(businessId);
        String businessType = account.profile() == null ? null : account.profile().businessType();

        return new OnboardingCatalogResponse(
                OnboardingCatalog.businessTypes(),
                OnboardingCatalog.servicesFor(businessType)
        );
    }

    @PutMapping("/business")
    public OnboardingSummary updateBusiness(
            @PathVariable String businessId,
            @Valid @RequestBody BusinessInformationRequest request) {

        requireWriteAccess(businessId);
        onboardingService.updateBusinessInformation(
                businessId,
                request.businessName(),
                request.businessType(),
                request.description());

        return requireSummary(businessId);
    }

    @PutMapping("/services")
    public OnboardingSummary updateServices(
            @PathVariable String businessId,
            @Valid @RequestBody ServicesRequest request) {

        requireWriteAccess(businessId);
        onboardingService.updateServices(businessId, request.services());
        return requireSummary(businessId);
    }

    @PutMapping("/knowledge")
    public OnboardingSummary updateKnowledge(
            @PathVariable String businessId,
            @Valid @RequestBody KnowledgeRequest request) {

        requireWriteAccess(businessId);
        onboardingService.updateKnowledge(businessId, request.knowledge());
        return requireSummary(businessId);
    }

    @PostMapping("/complete")
    public OnboardingSummary complete(@PathVariable String businessId) {
        requireWriteAccess(businessId);
        return onboardingService.complete(businessId);
    }

    private void requireAccess(String businessId) {
        tenantAuthorization.requireBusinessAccess(businessId);
    }

    private void requireWriteAccess(String businessId) {
        tenantAuthorization.requireBusinessWriteAccess(businessId);
    }

    private BusinessAccount requireAccount(String businessId) {
        BusinessAccount account = businessAccountRepository.findByBusinessId(businessId);
        if (account == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Business not found");
        }
        return account;
    }

    private OnboardingSummary requireSummary(String businessId) {
        OnboardingSummary summary = onboardingService.getOnboardingSummary(businessId);
        if (summary == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Business not found");
        }
        return summary;
    }

    public record OnboardingCatalogResponse(
            List<OnboardingCatalog.BusinessTypeOption> businessTypes,
            List<OnboardingCatalog.ServiceTemplate> suggestedServices) {
    }

    public record BusinessInformationRequest(
            @NotBlank @Size(max = 200) String businessName,
            @NotBlank @Size(max = 100) String businessType,
            @Size(max = 5000) String description) {
    }

    public record ServicesRequest(
            @Valid List<RequestDefinition> services) {
    }

    public record KnowledgeRequest(
            @Valid BusinessKnowledge knowledge) {
    }
}
