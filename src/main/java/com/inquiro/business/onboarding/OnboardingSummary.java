package com.inquiro.business.onboarding;

public record OnboardingSummary(
        String businessId,
        OnboardingStatus status,
        boolean businessInformationComplete,
        boolean servicesConfigured,
        boolean knowledgeConfigured,
        boolean channelConfigured,
        boolean readyForReceptionist
) {
}