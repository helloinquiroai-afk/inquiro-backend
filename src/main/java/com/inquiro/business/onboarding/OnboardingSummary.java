package com.inquiro.business.onboarding;

import java.util.List;

public record OnboardingSummary(
        String businessId,
        OnboardingStatus status,
        boolean businessInformationComplete,
        boolean servicesConfigured,
        boolean knowledgeConfigured,
        boolean channelConfigured,
        boolean readyForReceptionist,
        List<String> missingRequirements
) {
    public OnboardingSummary {
        missingRequirements =
                missingRequirements == null
                        ? List.of()
                        : List.copyOf(missingRequirements);
    }

    // Backward-compatible constructor
    public OnboardingSummary(
            String businessId,
            OnboardingStatus status,
            boolean businessInformationComplete,
            boolean servicesConfigured,
            boolean knowledgeConfigured,
            boolean channelConfigured,
            boolean readyForReceptionist) {

        this(
                businessId,
                status,
                businessInformationComplete,
                servicesConfigured,
                knowledgeConfigured,
                channelConfigured,
                readyForReceptionist,
                List.of()
        );
    }
}