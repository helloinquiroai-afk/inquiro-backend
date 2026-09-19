package com.inquiro.business;

import com.inquiro.auth.TenantAuthorizationService;
import com.inquiro.business.onboarding.*;
import com.inquiro.request.RequestDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessPlatformRegressionTest {

    @Test
    void onboardingStepsExposeIncompletePlatformAreas() {
        BusinessAccountRepository accounts = mock(BusinessAccountRepository.class);
        BusinessChannelRepository channels = mock(BusinessChannelRepository.class);

        BusinessProfile profile = new BusinessProfile(
                "Hotel",
                "HOSPITALITY",
                "Hotel",
                List.of(new RequestDefinition(
                        "ROOM_BOOKING",
                        "Room",
                        List.of("checkInDate"),
                        Map.of())),
                new BusinessKnowledge(
                        "Hotel",
                        List.of("ROOM_BOOKING"),
                        List.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        "Use approved facts only"));

        when(accounts.findByBusinessId("biz-1"))
                .thenReturn(new BusinessAccount("biz-1", "Hotel", profile));
        when(channels.findByBusinessId("biz-1")).thenReturn(List.of());

        OnboardingService service = new OnboardingService(accounts, channels);
        OnboardingSummary summary = service.getOnboardingSummary("biz-1");

        assertEquals(OnboardingStatus.IN_PROGRESS, summary.status());
        assertTrue(summary.businessInformationComplete());
        assertTrue(summary.servicesConfigured());
        assertTrue(summary.knowledgeConfigured());
        assertFalse(summary.channelConfigured());
        assertFalse(summary.readyForReceptionist());
        assertEquals(List.of("CHANNEL"), summary.missingRequirements());

        assertEquals(4, service.getOnboardingSteps("biz-1").size());
        assertFalse(service.getOnboardingSteps("biz-1").get(3).complete());
    }

    @Test
    void onboardingBecomesCompleteWhenEnabledChannelExists() {
        BusinessAccountRepository accounts = mock(BusinessAccountRepository.class);
        BusinessChannelRepository channels = mock(BusinessChannelRepository.class);

        BusinessProfile profile = new BusinessProfile(
                "Clinic",
                "HEALTHCARE",
                "Clinic",
                List.of(new RequestDefinition(
                        "DOCTOR_APPOINTMENT",
                        "Appointment",
                        List.of("date", "time"),
                        Map.of())),
                new BusinessKnowledge(
                        "Clinic",
                        List.of("DOCTOR_APPOINTMENT"),
                        List.of(),
                        Map.of("phone", "123"),
                        List.of(),
                        List.of(),
                        "Approved information"));

        when(accounts.findByBusinessId("biz-2"))
                .thenReturn(new BusinessAccount("biz-2", "Clinic", profile));
        when(channels.findByBusinessId("biz-2")).thenReturn(List.of(
                new BusinessChannel(
                        "ch-1", "biz-2", BusinessChannelType.WEBSITE, "clinic-web", true)));

        OnboardingSummary summary = new OnboardingService(accounts, channels)
                .getOnboardingSummary("biz-2");

        assertEquals(OnboardingStatus.COMPLETED, summary.status());
        assertTrue(summary.readyForReceptionist());
        assertTrue(summary.missingRequirements().isEmpty());
    }

    @Test
    void channelLookupIsScopedToBusiness() {
        BusinessChannelRepository repository = new BusinessChannelRepository() {
            private final List<BusinessChannel> channels = List.of(
                    new BusinessChannel(
                            "a", "biz-a", BusinessChannelType.WEBSITE, "same-site", true),
                    new BusinessChannel(
                            "b", "biz-b", BusinessChannelType.WEBSITE, "other-site", true));

            @Override
            public BusinessChannel findByTypeAndExternalId(
                    BusinessChannelType type, String externalId) {
                return null;
            }

            @Override
            public List<BusinessChannel> findByBusinessId(String businessId) {
                return channels.stream()
                        .filter(channel -> channel.businessId().equals(businessId))
                        .toList();
            }

            @Override
            public void save(BusinessChannel channel) {
            }
        };

        assertNotNull(repository.findByBusinessIdAndTypeAndExternalId(
                "biz-a", BusinessChannelType.WEBSITE, "same-site"));
        assertNull(repository.findByBusinessIdAndTypeAndExternalId(
                "biz-b", BusinessChannelType.WEBSITE, "same-site"));
    }

    @Test
    void dashboardReportsServicesAndChannelState() {
        BusinessAccountRepository accounts = mock(BusinessAccountRepository.class);
        BusinessChannelRepository channels = mock(BusinessChannelRepository.class);
        TenantAuthorizationService auth = mock(TenantAuthorizationService.class);
        OnboardingService onboarding = mock(OnboardingService.class);

        BusinessProfile profile = new BusinessProfile(
                "Auto Care",
                "AUTOMOTIVE",
                "Auto",
                List.of(
                        new RequestDefinition("CAR_SERVICE", "Service", List.of()),
                        new RequestDefinition("PARTS_SALES", "Parts", List.of())),
                BusinessKnowledge.empty());

        when(accounts.findByBusinessId("biz-auto"))
                .thenReturn(new BusinessAccount("biz-auto", "Auto Care", profile));
        when(channels.findByBusinessId("biz-auto")).thenReturn(List.of(
                new BusinessChannel("c1", "biz-auto", BusinessChannelType.WEBSITE, "web", true),
                new BusinessChannel("c2", "biz-auto", BusinessChannelType.WEBSITE, "web2", false)));

        OnboardingSummary onboardingSummary = new OnboardingSummary(
                "biz-auto",
                OnboardingStatus.IN_PROGRESS,
                true,
                true,
                false,
                true,
                false,
                List.of("KNOWLEDGE"));
        when(onboarding.getOnboardingSummary("biz-auto")).thenReturn(onboardingSummary);

        BusinessDashboardController controller =
                new BusinessDashboardController(accounts, channels, auth, onboarding);

        BusinessDashboardController.BusinessDashboardResponse response =
                controller.getDashboard("biz-auto");

        assertEquals("biz-auto", response.businessId());
        assertEquals(2, response.serviceCount());
        assertEquals(2, response.channelCount());
        assertEquals(1, response.enabledChannelCount());
        assertEquals(OnboardingStatus.IN_PROGRESS, response.onboarding().status());
        verify(auth).requireBusinessAccess("biz-auto");
    }
}
