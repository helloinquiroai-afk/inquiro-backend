package com.inquiro.business.onboarding;

import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessBoundaries;
import com.inquiro.business.BusinessChannel;
import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessChannelType;
import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnboardingServiceTest {

    private BusinessAccountRepository businessAccountRepository;

    private BusinessChannelRepository businessChannelRepository;

    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {

        businessAccountRepository =
                mock(BusinessAccountRepository.class);

        businessChannelRepository =
                mock(BusinessChannelRepository.class);

        onboardingService =
                new OnboardingService(
                        businessAccountRepository,
                        businessChannelRepository
                );
    }

    @Test
    void shouldReturnNullWhenBusinessDoesNotExist() {

        String businessId = "biz_missing";

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(null);

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertNull(result);
    }

    @Test
    void shouldReturnNotStartedForEmptyBusiness() {

        String businessId = "biz_001";

        BusinessProfile profile =
                new BusinessProfile(
                        "",
                        "",
                        "",
                        List.of(),
                        BusinessKnowledge.empty()
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "",
                        profile
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of());

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertEquals(
                OnboardingStatus.NOT_STARTED,
                result.status()
        );

        assertFalse(
                result.businessInformationComplete()
        );

        assertFalse(
                result.servicesConfigured()
        );

        assertFalse(
                result.knowledgeConfigured()
        );

        assertFalse(
                result.channelConfigured()
        );

        assertFalse(
                result.readyForReceptionist()
        );
    }

    @Test
    void shouldBeInProgressWhenBusinessInformationIsConfigured() {

        String businessId = "biz_002";

        BusinessProfile profile =
                new BusinessProfile(
                        "Ocean View Hotel",
                        "HOTEL",
                        "A hotel in Matara",
                        List.of(),
                        BusinessKnowledge.empty()
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "Ocean View Hotel",
                        profile
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of());

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertEquals(
                OnboardingStatus.IN_PROGRESS,
                result.status()
        );

        assertTrue(
                result.businessInformationComplete()
        );

        assertFalse(
                result.servicesConfigured()
        );

        assertFalse(
                result.knowledgeConfigured()
        );

        assertFalse(
                result.channelConfigured()
        );

        assertFalse(
                result.readyForReceptionist()
        );
    }

    @Test
    void shouldRecognizeConfiguredService() {

        String businessId = "biz_003";

        RequestDefinition service =
                new RequestDefinition(
                        "ROOM_BOOKING",
                        "Book a hotel room",
                        List.of("checkInDate", "guestCount")
                );

        BusinessProfile profile =
                new BusinessProfile(
                        "Ocean View Hotel",
                        "HOTEL",
                        "A hotel in Matara",
                        List.of(service),
                        BusinessKnowledge.empty()
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "Ocean View Hotel",
                        profile
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of());

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertTrue(
                result.businessInformationComplete()
        );

        assertTrue(
                result.servicesConfigured()
        );

        assertEquals(
                OnboardingStatus.IN_PROGRESS,
                result.status()
        );
    }

    @Test
    void shouldNotRecognizeServiceWithBlankRequestType() {

        String businessId = "biz_004";

        RequestDefinition service =
                new RequestDefinition(
                        "",
                        "Book a hotel room",
                        List.of("checkInDate")
                );

        BusinessProfile profile =
                new BusinessProfile(
                        "Ocean View Hotel",
                        "HOTEL",
                        "A hotel in Matara",
                        List.of(service),
                        BusinessKnowledge.empty()
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "Ocean View Hotel",
                        profile
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of());

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertFalse(
                result.servicesConfigured()
        );
    }

    @Test
    void shouldRecognizeMeaningfulKnowledge() {

        String businessId = "biz_005";

        BusinessKnowledge knowledge =
                new BusinessKnowledge(
                        "A family hotel in Matara",
                        List.of(),
                        List.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        ""
                );

        BusinessProfile profile =
                new BusinessProfile(
                        "Ocean View Hotel",
                        "HOTEL",
                        "Hotel",
                        List.of(),
                        knowledge
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "Ocean View Hotel",
                        profile
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of());

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertTrue(
                result.knowledgeConfigured()
        );
    }

    @Test
    void shouldRecognizeConfiguredChannel() {

        String businessId = "biz_006";

        BusinessProfile profile =
                new BusinessProfile(
                        "",
                        "",
                        "",
                        List.of(),
                        BusinessKnowledge.empty()
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "",
                        profile
                );

        BusinessChannel channel =
                new BusinessChannel(
                        "channel_001",
                        businessId,
                        BusinessChannelType.WEBSITE,
                        "website_001",
                        true
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of(channel));

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertTrue(
                result.channelConfigured()
        );

        assertEquals(
                OnboardingStatus.IN_PROGRESS,
                result.status()
        );
    }

    @Test
    void shouldIgnoreDisabledChannels() {

        String businessId = "biz_007";

        BusinessProfile profile =
                new BusinessProfile(
                        "",
                        "",
                        "",
                        List.of(),
                        BusinessKnowledge.empty()
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "",
                        profile
                );

        BusinessChannel channel =
                new BusinessChannel(
                        "channel_001",
                        businessId,
                        BusinessChannelType.WEBSITE,
                        "website_001",
                        false
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of(channel));

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertFalse(
                result.channelConfigured()
        );
    }

    @Test
    void shouldBeCompletedWhenAllRequirementsAreConfigured() {

        String businessId = "biz_008";

        RequestDefinition service =
                new RequestDefinition(
                        "ROOM_BOOKING",
                        "Book a hotel room",
                        List.of(
                                "checkInDate",
                                "guestCount"
                        )
                );

        BusinessKnowledge knowledge =
                new BusinessKnowledge(
                        "A family hotel in Matara",
                        List.of("Rooms"),
                        List.of(),
                        Map.of(
                                "checkInTime",
                                "2 PM"
                        ),
                        List.of(
                                "Do you provide breakfast?"
                        ),
                        List.of(
                                "Cancellation allowed 24 hours before arrival"
                        ),
                        "Be friendly and helpful"
                );

        BusinessProfile profile =
                new BusinessProfile(
                        "Ocean View Hotel",
                        "HOTEL",
                        "A hotel in Matara",
                        List.of(service),
                        knowledge
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "Ocean View Hotel",
                        profile
                );

        BusinessChannel channel =
                new BusinessChannel(
                        "channel_001",
                        businessId,
                        BusinessChannelType.WEBSITE,
                        "website_001",
                        true
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of(channel));

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertEquals(
                OnboardingStatus.COMPLETED,
                result.status()
        );

        assertTrue(
                result.businessInformationComplete()
        );

        assertTrue(
                result.servicesConfigured()
        );

        assertTrue(
                result.knowledgeConfigured()
        );

        assertTrue(
                result.channelConfigured()
        );

        assertTrue(
                result.readyForReceptionist()
        );
    }

    @Test
    void shouldRemainInProgressWhenOneRequirementIsMissing() {

        String businessId = "biz_009";

        RequestDefinition service =
                new RequestDefinition(
                        "ROOM_BOOKING",
                        "Book a hotel room",
                        List.of("checkInDate")
                );

        BusinessKnowledge knowledge =
                new BusinessKnowledge(
                        "A family hotel in Matara",
                        List.of(),
                        List.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        ""
                );

        BusinessProfile profile =
                new BusinessProfile(
                        "Ocean View Hotel",
                        "HOTEL",
                        "Hotel",
                        List.of(service),
                        knowledge
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "Ocean View Hotel",
                        profile
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        // No channel configured.
        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of());

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertEquals(
                OnboardingStatus.IN_PROGRESS,
                result.status()
        );

        assertTrue(
                result.businessInformationComplete()
        );

        assertTrue(
                result.servicesConfigured()
        );

        assertTrue(
                result.knowledgeConfigured()
        );

        assertFalse(
                result.channelConfigured()
        );

        assertFalse(
                result.readyForReceptionist()
        );
    }

    @Test
    void shouldAcceptAnyEnabledChannelType() {

        String businessId = "biz_010";

        BusinessProfile profile =
                new BusinessProfile(
                        "Ocean View Hotel",
                        "HOTEL",
                        "",
                        List.of(),
                        BusinessKnowledge.empty()
                );

        BusinessAccount account =
                new BusinessAccount(
                        businessId,
                        "Ocean View Hotel",
                        profile
                );

        BusinessChannel channel =
                new BusinessChannel(
                        "channel_001",
                        businessId,
                        BusinessChannelType.MESSENGER,
                        "page_123",
                        true
                );

        when(
                businessAccountRepository
                        .findByBusinessId(businessId)
        ).thenReturn(account);

        when(
                businessChannelRepository
                        .findByBusinessId(businessId)
        ).thenReturn(List.of(channel));

        OnboardingSummary result =
                onboardingService.getOnboardingSummary(businessId);

        assertTrue(
                result.channelConfigured()
        );
    }
}