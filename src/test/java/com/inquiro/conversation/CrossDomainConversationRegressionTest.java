package com.inquiro.conversation;

import com.inquiro.ai.AiService;
import com.inquiro.ai.ConversationIntentAnalysis;
import com.inquiro.ai.FollowUpAnalysis;
import com.inquiro.ai.RequestAnalysis;
import com.inquiro.availability.AvailabilityService;
import com.inquiro.booking.BookingCreationService;
import com.inquiro.booking.BookingEntity;
import com.inquiro.business.*;
import com.inquiro.business.onboarding.OnboardingService;
import com.inquiro.business.onboarding.OnboardingStatus;
import com.inquiro.business.onboarding.OnboardingSummary;
import com.inquiro.inquiry.InquiryOrchestrator;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;
import com.inquiro.inquiry.InquiryStatus;
import com.inquiro.request.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CrossDomainConversationRegressionTest {

    private static final String SESSION = "customer-1";

    private AiService ai;
    private InquiryOrchestrator orchestrator;
    private SlotFillingEngine slotFillingEngine;
    private BusinessAccountRepository accounts;
    private BusinessChannelRepository channels;
    private BusinessRequestService businessRequests;
    private BusinessBoundaryService boundaries;
    private OnboardingService onboarding;
    private BookingCreationService bookingCreation;
    private ConversationRepository conversations;
    private RequestActionHandlerRegistry actions;

    @BeforeEach
    void setUp() {
        ai = mock(AiService.class);
        orchestrator = mock(InquiryOrchestrator.class);
        slotFillingEngine = mock(SlotFillingEngine.class);
        accounts = mock(BusinessAccountRepository.class);
        channels = mock(BusinessChannelRepository.class);
        businessRequests = mock(BusinessRequestService.class);
        boundaries = mock(BusinessBoundaryService.class);
        onboarding = mock(OnboardingService.class);
        bookingCreation = mock(BookingCreationService.class);
        conversations = mock(ConversationRepository.class);

        actions = new RequestActionHandlerRegistry(List.of(
                new com.inquiro.booking.BookingRequestActionHandler(bookingCreation, conversations),
                new BusinessRequestActionHandler(businessRequests, conversations),
                new HumanReviewActionHandler(businessRequests, conversations)
        ));

        when(onboarding.getOnboardingSummary(anyString())).thenReturn(
                new OnboardingSummary(
                        "test-business",
                        OnboardingStatus.COMPLETED,
                        true, true, true, true, true
                )
        );

        when(conversations.find(anyString())).thenReturn(null);
        when(channels.findByTypeAndExternalId(any(), anyString())).thenAnswer(invocation -> {
            String externalId = (String) invocation.getArgument(1);
            return new BusinessChannel(
                    "channel-" + externalId,
                    "biz-" + externalId.replaceFirst("^website-", ""),
                    invocation.getArgument(0),
                    externalId,
                    true
            );
        });
    }

    static Stream<DomainScenario> domains() {
        return Stream.of(
                new DomainScenario(
                        "hotel",
                        "HOTEL",
                        "ROOM_BOOKING",
                        RequestActionType.BOOKING,
                        List.of("location", "checkInDate", "guestCount", "time"),
                        Map.of(
                                "location", "Paris",
                                "checkInDate", LocalDate.now().plusDays(7).toString(),
                                "guestCount", 2,
                                "time", "14:00",
                                "customerName", "Alex",
                                "customerPhone", "0712345678"
                        )
                ),
                new DomainScenario(
                        "restaurant",
                        "RESTAURANT",
                        "TABLE_RESERVATION",
                        RequestActionType.BOOKING,
                        List.of("location", "date", "time"),
                        Map.of(
                                "location", "Matara",
                                "date", LocalDate.now().plusDays(2).toString(),
                                "time", "19:00",
                                "guestCount", 4,
                                "customerName", "Alex",
                                "customerPhone", "0712345678"
                        )
                ),
                new DomainScenario(
                        "healthcare",
                        "HEALTHCARE",
                        "DOCTOR_APPOINTMENT",
                        RequestActionType.BOOKING,
                        List.of("specialty", "date", "time"),
                        Map.of(
                                "specialty", "Dentist",
                                "date", LocalDate.now().plusDays(3).toString(),
                                "time", "10:00",
                                "customerName", "Alex",
                                "customerPhone", "0712345678"
                        )
                ),
                new DomainScenario(
                        "auto",
                        "AUTOMOTIVE",
                        "CAR_SERVICE",
                        RequestActionType.BUSINESS_REQUEST,
                        List.of("vehicleNumber", "serviceType"),
                        Map.of(
                                "vehicleNumber", "CAB-1234",
                                "serviceType", "Brake inspection"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0} routes a completed request through its configured action")
    @MethodSource("domains")
    void completedRequestsRouteByDomainConfiguration(DomainScenario scenario) {
        BusinessProfile profile = profile(scenario);
        BusinessAccount account = new BusinessAccount(
                "biz-" + scenario.name(),
                scenario.name() + " business",
                profile
        );
        when(accounts.findByBusinessId(account.businessId())).thenReturn(account);

        InquiryResult inquiry = new InquiryResult(
                scenario.domain(),
                scenario.service(),
                scenario.fields()
        );
        when(orchestrator.process("complete request", profile)).thenReturn(
                new InquiryResponse(
                        inquiry,
                        List.of(),
                        InquiryStatus.INFORMATION_COLLECTED,
                        "ready"
                )
        );
        when(boundaries.check(scenario.service(), profile)).thenReturn(
                new BusinessBoundaryService.BoundaryResult(
                        BusinessBoundaryService.BoundaryStatus.SUPPORTED,
                        null
                )
        );

        if (scenario.actionType() == RequestActionType.BOOKING) {
            BookingEntity booking = new BookingEntity(
                    "booking-" + scenario.name(),
                    account.businessId(),
                    scenario.service(),
                    LocalDate.now().plusDays(1),
                    null,
                    null,
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 0),
                    "Alex",
                    "0712345678",
                    com.inquiro.booking.BookingStatus.CONFIRMED,
                    LocalDateTime.now()
            );
            when(bookingCreation.create(
                    eq(account.businessId()),
                    eq(scenario.service()),
                    eq(scenario.fields()),
                    eq("Alex"),
                    eq("0712345678"),
                    anyString()
            )).thenReturn(booking);
        }

        ConversationService service = conversationService();

        InquiryResponse response = service.process(
                SESSION,
                BusinessChannelType.WEBSITE,
                "website-" + scenario.name(),
                "complete request"
        );

        assertEquals(InquiryStatus.INFORMATION_COLLECTED, response.status());

        if (scenario.actionType() == RequestActionType.BOOKING) {
            assertTrue(response.reply().contains("booking-" + scenario.name()));
            verify(bookingCreation).create(
                    eq(account.businessId()),
                    eq(scenario.service()),
                    eq(scenario.fields()),
                    eq("Alex"),
                    eq("0712345678"),
                    anyString()
            );
            verify(businessRequests, never()).create(anyString(), anyString(), anyString(), anyMap(), any());
        } else {
            assertEquals("Your request has been received.", response.reply());
            verify(businessRequests).create(
                    eq(account.businessId()),
                    anyString(),
                    eq(scenario.service()),
                    eq(scenario.fields()),
                    any()
            );
            verifyNoInteractions(bookingCreation);
        }

        verify(conversations).remove(anyString());
    }

    @Test
    void configuredHumanReviewActionIsUsedWithoutBookingFields() {
        DomainScenario scenario = new DomainScenario(
                "legal",
                "LEGAL",
                "INSURANCE_CLAIM",
                RequestActionType.HUMAN_REVIEW,
                List.of("claimNumber", "description"),
                Map.of(
                        "claimNumber", "CLM-42",
                        "description", "Please review my claim"
                )
        );
        BusinessProfile profile = profile(scenario);
        BusinessAccount account = new BusinessAccount("biz-legal", "Legal business", profile);
        when(accounts.findByBusinessId(account.businessId())).thenReturn(account);

        InquiryResult inquiry = new InquiryResult(
                scenario.domain(), scenario.service(), scenario.fields());
        when(orchestrator.process("review this", profile)).thenReturn(
                new InquiryResponse(inquiry, List.of(), InquiryStatus.INFORMATION_COLLECTED, "ready"));
        when(boundaries.check(scenario.service(), profile)).thenReturn(
                new BusinessBoundaryService.BoundaryResult(
                        BusinessBoundaryService.BoundaryStatus.SUPPORTED, null));

        InquiryResponse response = conversationService().process(
                SESSION, BusinessChannelType.WEBSITE, "website-legal", "review this");

        assertEquals("Your request has been received and will be reviewed by the business.", response.reply());
        verify(businessRequests).createForHumanReview(
                eq(account.businessId()),
                anyString(),
                eq("INSURANCE_CLAIM"),
                eq(scenario.fields())
        );
        verifyNoInteractions(bookingCreation);
    }

    @Test
    void incompleteBookingDoesNotCreateAnActionUntilAllSlotsAreCollected() {
        DomainScenario scenario = domains().findFirst().orElseThrow();
        BusinessProfile profile = profile(scenario);
        BusinessAccount account = new BusinessAccount("biz-progressive", "Hotel", profile);
        when(accounts.findByBusinessId(account.businessId())).thenReturn(account);

        InquiryResult firstInquiry = new InquiryResult(
                scenario.domain(), scenario.service(),
                Map.of("location", "Paris")
        );
        when(orchestrator.process("I need a room in Paris", profile)).thenReturn(
                new InquiryResponse(
                        firstInquiry,
                        List.of("checkInDate", "guestCount"),
                        InquiryStatus.NEEDS_INFORMATION,
                        "When would you like to check in?"
                )
        );

        InquiryResponse response = conversationService().process(
                SESSION, BusinessChannelType.WEBSITE, "website-progressive", "I need a room in Paris");

        assertEquals(InquiryStatus.NEEDS_INFORMATION, response.status());
        assertEquals(List.of("checkInDate", "guestCount"), response.missingFields());
        verifyNoInteractions(bookingCreation);
        verifyNoInteractions(businessRequests);
        verify(conversations).save(any(ConversationSession.class));
    }

    private ConversationService conversationService() {
        return new ConversationService(
                conversations,
                orchestrator,
                ai,
                slotFillingEngine,
                accounts,
                channels,
                businessRequests,
                boundaries,
                onboarding,
                bookingCreation,
                actions
        );
    }

    private BusinessProfile profile(DomainScenario scenario) {
        RequestDefinition definition = new RequestDefinition(
                scenario.service(),
                scenario.service() + " service",
                scenario.requiredSlots(),
                Map.of(),
                scenario.actionType(),
                scenario.actionType() == RequestActionType.BOOKING
                        ? List.of("customerName", "customerPhone")
                        : List.of(),
                scenario.actionType() == RequestActionType.BOOKING ? "AUTO" : "NONE"
        );

        return new BusinessProfile(
                scenario.name() + " business",
                scenario.domain(),
                "Test business for cross-domain regression coverage",
                List.of(definition),
                new BusinessKnowledge(
                        "Test business",
                        List.of(scenario.service()),
                        List.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        "Test instructions"
                )
        );
    }

    private record DomainScenario(
            String name,
            String domain,
            String service,
            RequestActionType actionType,
            List<String> requiredSlots,
            Map<String, Object> fields
    ) {
        @Override
        public String toString() {
            return name;
        }
    }
}
