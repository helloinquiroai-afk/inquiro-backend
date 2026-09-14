package com.inquiro.business.onboarding;

import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessBoundaries;
import com.inquiro.business.BusinessChannel;
import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final BusinessAccountRepository businessAccountRepository;
    private final BusinessChannelRepository businessChannelRepository;

    public OnboardingSummary getOnboardingSummary(String businessId) {

        BusinessAccount account =
                businessAccountRepository.findByBusinessId(businessId);

        if (account == null) {
            return null;
        }

        BusinessProfile profile = account.profile();

        boolean businessInformationComplete =
                isBusinessInformationComplete(profile);

        boolean servicesConfigured =
                areServicesConfigured(profile);

        boolean knowledgeConfigured =
                hasMeaningfulKnowledge(
                        profile == null ? null : profile.knowledge()
                );

        boolean channelConfigured =
                hasEnabledChannel(businessId);

        boolean readyForReceptionist =
                businessInformationComplete
                        && servicesConfigured
                        && knowledgeConfigured
                        && channelConfigured;

        OnboardingStatus status;

        if (readyForReceptionist) {
            status = OnboardingStatus.COMPLETED;
        } else if (
                businessInformationComplete
                        || servicesConfigured
                        || knowledgeConfigured
                        || channelConfigured
        ) {
            status = OnboardingStatus.IN_PROGRESS;
        } else {
            status = OnboardingStatus.NOT_STARTED;
        }

        return new OnboardingSummary(
                businessId,
                status,
                businessInformationComplete,
                servicesConfigured,
                knowledgeConfigured,
                channelConfigured,
                readyForReceptionist
        );
    }

    private boolean isBusinessInformationComplete(
            BusinessProfile profile) {

        if (profile == null) {
            return false;
        }

        return hasText(profile.businessName())
                && hasText(profile.businessType());
    }

    private boolean areServicesConfigured(
            BusinessProfile profile) {

        if (profile == null || profile.services() == null) {
            return false;
        }

        return profile.services()
                .stream()
                .anyMatch(this::isValidService);
    }

    private boolean isValidService(
            RequestDefinition service) {

        if (service == null) {
            return false;
        }

        return hasText(service.requestType());
    }

    private boolean hasEnabledChannel(
            String businessId) {

        List<BusinessChannel> channels =
                businessChannelRepository.findByBusinessId(businessId);

        if (channels == null || channels.isEmpty()) {
            return false;
        }

        return channels.stream()
                .anyMatch(channel ->
                        channel != null
                                && channel.enabled());
    }

    private boolean hasMeaningfulKnowledge(
            BusinessKnowledge knowledge) {

        if (knowledge == null) {
            return false;
        }

        if (hasText(knowledge.businessDescription())) {
            return true;
        }

        if (hasItems(knowledge.services())) {
            return true;
        }

        if (hasItems(knowledge.products())) {
            return true;
        }

        if (hasEntries(knowledge.facts())) {
            return true;
        }

        if (hasItems(knowledge.faqs())) {
            return true;
        }

        if (hasItems(knowledge.policies())) {
            return true;
        }

        if (hasText(knowledge.instructions())) {
            return true;
        }

        if (hasEntries(knowledge.operatingHours())) {
            return true;
        }

        if (hasItems(knowledge.locations())) {
            return true;
        }

        if (hasEntries(knowledge.contactInformation())) {
            return true;
        }

        if (hasEntries(knowledge.bookingRules())) {
            return true;
        }

        if (hasItems(knowledge.capabilities())) {
            return true;
        }

        if (hasItems(knowledge.restrictions())) {
            return true;
        }

        return hasMeaningfulBoundaries(
                knowledge.boundaries()
        );
    }

    private boolean hasMeaningfulBoundaries(
            BusinessBoundaries boundaries) {

        if (boundaries == null) {
            return false;
        }

        return hasItems(boundaries.supported())
                || hasItems(boundaries.notSupported())
                || hasItems(boundaries.requiresHuman());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasItems(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private boolean hasEntries(Map<?, ?> values) {
        return values != null && !values.isEmpty();
    }
}