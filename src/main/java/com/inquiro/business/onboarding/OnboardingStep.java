package com.inquiro.business.onboarding;

import java.util.List;

public record OnboardingStep(
        String key,
        String title,
        String description,
        boolean complete,
        List<String> missingRequirements
) {
    public OnboardingStep {
        missingRequirements = missingRequirements == null
                ? List.of()
                : List.copyOf(missingRequirements);
    }
}
