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

import java.util.ArrayList;
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

        List<String> missingRequirements =
                new ArrayList<>();

        if (!businessInformationComplete) {
            missingRequirements.add("BUSINESS_INFORMATION");
        }

        if (!servicesConfigured) {
            missingRequirements.add("SERVICES");
        }

        if (!knowledgeConfigured) {
            missingRequirements.add("KNOWLEDGE");
        }

        if (!channelConfigured) {
            missingRequirements.add("CHANNEL");
        }

        return new OnboardingSummary(
                businessId,
                status,
                businessInformationComplete,
                servicesConfigured,
                knowledgeConfigured,
                channelConfigured,
                readyForReceptionist,
                missingRequirements
        );
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateBusinessInformation(
            String businessId,
            String businessName,
            String businessType,
            String description) {

        BusinessAccount account = requireAccountForUpdate(businessId);
        BusinessProfile current = account.profile();

        BusinessProfile updated = new BusinessProfile(
                businessName,
                businessType,
                description,
                current == null ? List.of() : current.services(),
                current == null ? BusinessKnowledge.empty() : current.knowledge());

        businessAccountRepository.save(
                new BusinessAccount(account.businessId(), businessName, updated));
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateServices(
            String businessId,
            List<RequestDefinition> services) {

        BusinessAccount account = requireAccountForUpdate(businessId);
        BusinessProfile current = account.profile();

        BusinessProfile updated = new BusinessProfile(
                current.businessName(),
                current.businessType(),
                current.description(),
                services == null ? List.of() : services,
                current.knowledge());

        businessAccountRepository.save(
                new BusinessAccount(account.businessId(), account.businessName(), updated));
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateKnowledge(
            String businessId,
            BusinessKnowledge knowledge) {

        BusinessAccount account = requireAccountForUpdate(businessId);
        BusinessProfile current = account.profile();

        BusinessProfile updated = new BusinessProfile(
                current.businessName(),
                current.businessType(),
                current.description(),
                current.services(),
                knowledge == null ? BusinessKnowledge.empty() : knowledge);

        businessAccountRepository.save(
                new BusinessAccount(account.businessId(), account.businessName(), updated));
    }

    public OnboardingSummary complete(String businessId) {
        OnboardingSummary summary = getOnboardingSummary(businessId);
        if (summary == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Business not found");
        }

        if (!summary.readyForReceptionist()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Onboarding is incomplete: " + String.join(", ", summary.missingRequirements()));
        }

        return summary;
    }

    private BusinessAccount requireAccountForUpdate(String businessId) {
        BusinessAccount account =
                businessAccountRepository.findByBusinessIdForUpdate(businessId);

        if (account == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Business not found");
        }

        if (account.profile() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Business profile is not initialized");
        }

        return account;
    }

    public List<OnboardingStep> getOnboardingSteps(String businessId) {

        OnboardingSummary summary = getOnboardingSummary(businessId);

        if (summary == null) {
            return List.of();
        }

        return List.of(
                new OnboardingStep(
                        "BUSINESS_INFORMATION",
                        "Business information",
                        "Configure the business name and business type.",
                        summary.businessInformationComplete(),
                        List.of("BUSINESS_INFORMATION")
                                .stream()
                                .filter(key -> summary.missingRequirements().contains(key))
                                .toList()
                ),
                new OnboardingStep(
                        "SERVICES",
                        "Services",
                        "Define the services the receptionist can handle.",
                        summary.servicesConfigured(),
                        List.of("SERVICES")
                                .stream()
                                .filter(key -> summary.missingRequirements().contains(key))
                                .toList()
                ),
                new OnboardingStep(
                        "KNOWLEDGE",
                        "Business knowledge",
                        "Provide approved information, policies, FAQs, hours and other facts.",
                        summary.knowledgeConfigured(),
                        List.of("KNOWLEDGE")
                                .stream()
                                .filter(key -> summary.missingRequirements().contains(key))
                                .toList()
                ),
                new OnboardingStep(
                        "CHANNEL",
                        "Channel",
                        "Connect at least one enabled customer communication channel.",
                        summary.channelConfigured(),
                        List.of("CHANNEL")
                                .stream()
                                .filter(key -> summary.missingRequirements().contains(key))
                                .toList()
                )
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

        if (hasItems(knowledge.locationDetails())) {
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