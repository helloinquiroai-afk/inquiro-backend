package com.inquiro.business.onboarding;

import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessChannel;
import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnboardingServiceTest {

    private BusinessAccountRepository businessAccountRepository;
    private BusinessChannelRepository businessChannelRepository;
    private OnboardingService service;

    @BeforeEach
    void setup() {
        businessAccountRepository =
                mock(BusinessAccountRepository.class);

        businessChannelRepository =
                mock(BusinessChannelRepository.class);

        service =
                new OnboardingService(
                        businessAccountRepository,
                        businessChannelRepository
                );
    }

    @Test
    void returnsAllRequirementsMissingWhenNothingIsConfigured() {

        BusinessProfile profile =
                new BusinessProfile(
                        "",
                        "",
                        "",
                        List.of(),
                        BusinessKnowledge.empty()
                );

        when(
                businessAccountRepository.findByBusinessId("biz-1")
        ).thenReturn(
                new BusinessAccount(
                        "biz-1",
                        "",
                        profile
                )
        );

        when(
                businessChannelRepository.findByBusinessId("biz-1")
        ).thenReturn(List.of());

        OnboardingSummary summary =
                service.getOnboardingSummary("biz-1");

        assertEquals(
                OnboardingStatus.NOT_STARTED,
                summary.status()
        );

        assertFalse(
                summary.businessInformationComplete()
        );

        assertFalse(
                summary.servicesConfigured()
        );

        assertFalse(
                summary.knowledgeConfigured()
        );

        assertFalse(
                summary.channelConfigured()
        );

        assertFalse(
                summary.readyForReceptionist()
        );

        assertEquals(
                List.of(
                        "BUSINESS_INFORMATION",
                        "SERVICES",
                        "KNOWLEDGE",
                        "CHANNEL"
                ),
                summary.missingRequirements()
        );
    }

    @Test
    void returnsOnlyMissingRequirementsWhenPartiallyConfigured() {

        BusinessProfile profile =
                new BusinessProfile(
                        "Test Hotel",
                        "HOSPITALITY",
                        "",
                        List.of(
                                new RequestDefinition(
                                        "ROOM_BOOKING",
                                        "Room booking",
                                        List.of("location")
                                )
                        ),
                        BusinessKnowledge.empty()
                );

        when(
                businessAccountRepository.findByBusinessId("biz-1")
        ).thenReturn(
                new BusinessAccount(
                        "biz-1",
                        "Test Hotel",
                        profile
                )
        );

        when(
                businessChannelRepository.findByBusinessId("biz-1")
        ).thenReturn(List.of());

        OnboardingSummary summary =
                service.getOnboardingSummary("biz-1");

        assertEquals(
                OnboardingStatus.IN_PROGRESS,
                summary.status()
        );

        assertTrue(
                summary.businessInformationComplete()
        );

        assertTrue(
                summary.servicesConfigured()
        );

        assertFalse(
                summary.knowledgeConfigured()
        );

        assertFalse(
                summary.channelConfigured()
        );

        assertFalse(
                summary.readyForReceptionist()
        );

        assertEquals(
                List.of(
                        "KNOWLEDGE",
                        "CHANNEL"
                ),
                summary.missingRequirements()
        );
    }

    @Test
    void returnsNoMissingRequirementsWhenOnboardingIsComplete() {

        BusinessProfile profile =
                new BusinessProfile(
                        "Test Hotel",
                        "HOSPITALITY",
                        "A hotel in Matara",
                        List.of(
                                new RequestDefinition(
                                        "ROOM_BOOKING",
                                        "Room booking",
                                        List.of("location")
                                )
                        ),
                        new BusinessKnowledge(
                                "A hotel in Matara",
                                List.of("ROOM_BOOKING"),
                                List.of(),
                                Map.of("parking", "Free"),
                                List.of(),
                                List.of(),
                                ""
                        )
                );

        when(
                businessAccountRepository.findByBusinessId("biz-1")
        ).thenReturn(
                new BusinessAccount(
                        "biz-1",
                        "Test Hotel",
                        profile
                )
        );

        when(
                businessChannelRepository.findByBusinessId("biz-1")
        ).thenReturn(
                List.of(
                        new BusinessChannel(
                                "channel-1",
                                "biz-1",
                                com.inquiro.business.BusinessChannelType.WEBSITE,
                                "website-1",
                                true
                        )
                )
        );

        OnboardingSummary summary =
                service.getOnboardingSummary("biz-1");

        assertEquals(
                OnboardingStatus.COMPLETED,
                summary.status()
        );

        assertTrue(
                summary.businessInformationComplete()
        );

        assertTrue(
                summary.servicesConfigured()
        );

        assertTrue(
                summary.knowledgeConfigured()
        );

        assertTrue(
                summary.channelConfigured()
        );

        assertTrue(
                summary.readyForReceptionist()
        );

        assertTrue(
                summary.missingRequirements().isEmpty()
        );
    }

    @Test
    void returnsNullWhenBusinessDoesNotExist() {

        when(
                businessAccountRepository.findByBusinessId("unknown")
        ).thenReturn(null);

        OnboardingSummary summary =
                service.getOnboardingSummary("unknown");

        assertNull(summary);

        verify(
                businessChannelRepository,
                never()
        ).findByBusinessId(anyString());
    }
}