package com.inquiro.business;

import com.inquiro.auth.TenantAuthorizationService;
import com.inquiro.business.onboarding.OnboardingService;
import com.inquiro.business.onboarding.OnboardingSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business/accounts/{businessId}/dashboard")
@RequiredArgsConstructor
public class BusinessDashboardController {

    private final BusinessAccountRepository businessAccountRepository;
    private final BusinessChannelRepository businessChannelRepository;
    private final TenantAuthorizationService tenantAuthorization;
    private final OnboardingService onboardingService;

    @GetMapping
    public BusinessDashboardResponse getDashboard(
            @PathVariable String businessId) {

        tenantAuthorization.requireBusinessAccess(businessId);

        BusinessAccount account =
                businessAccountRepository.findByBusinessId(businessId);

        if (account == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Business not found"
            );
        }

        BusinessProfile profile = account.profile();
        OnboardingSummary onboarding =
                onboardingService.getOnboardingSummary(businessId);

        List<BusinessChannel> channels =
                businessChannelRepository.findByBusinessId(businessId);

        long enabledChannels = channels == null
                ? 0
                : channels.stream()
                        .filter(channel -> channel != null && channel.enabled())
                        .count();

        int serviceCount = profile == null || profile.services() == null
                ? 0
                : profile.services().size();

        return new BusinessDashboardResponse(
                businessId,
                account.businessName(),
                profile == null ? null : profile.businessType(),
                serviceCount,
                channels == null ? 0 : channels.size(),
                (int) enabledChannels,
                onboarding
        );
    }

    public record BusinessDashboardResponse(
            String businessId,
            String businessName,
            String businessType,
            int serviceCount,
            int channelCount,
            int enabledChannelCount,
            OnboardingSummary onboarding
    ) {
    }
}
