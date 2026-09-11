package com.inquiro.conversation;

import com.inquiro.ai.*;
import com.inquiro.availability.*;
import com.inquiro.business.*;
import com.inquiro.inquiry.*;
import com.inquiro.request.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConversationServiceTest {
    private AiService ai;
    private ConversationService service;
    private final Map<String, ConversationSession> sessions = new HashMap<>();
    private BusinessProfile profile;
    private BusinessChannelRepository channels;
    private BusinessRequestService requests;

    @BeforeEach void setup() {
        ai = mock(AiService.class);
        var provider = mock(BusinessProfileProvider.class);
        profile = new BusinessProfile("Test Hotel", "HOSPITALITY", "Hotel", List.of(
                new RequestDefinition("ROOM_BOOKING", "Room booking",
                        List.of("location", "checkInDate", "guestCount", "durationNights"),
                        Map.of("checkInDate", "When would you like to check in?", "durationNights", "How many nights?"))),
                new BusinessKnowledge("Hotel", List.of("ROOM_BOOKING"), List.of(), Map.of("parking", "Free"),
                        List.of(), List.of(), ""));
        when(provider.get()).thenReturn(profile);
        var slots = new SlotFillingEngine(provider);
        var availability = new AvailabilityService(List.of(new BusinessKnowledgeAvailabilitySource()));
        var boundary = new BusinessBoundaryService();
        var orchestrator = new InquiryOrchestrator(ai, slots, new RequestAnalysisValidator(), provider,
                new BusinessQuestionService(ai), availability, boundary);
        var repository = mock(ConversationRepository.class);
        when(repository.find(anyString())).thenAnswer(invocation -> sessions.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            ConversationSession session = invocation.getArgument(0);
            sessions.put(session.getSessionId(), session);
            return null;
        }).when(repository).save(any());
        doAnswer(invocation -> { sessions.remove(invocation.getArgument(0)); return null; }).when(repository).remove(anyString());
        channels = mock(BusinessChannelRepository.class);
        when(channels.findByTypeAndExternalId(any(), anyString())).thenAnswer(invocation ->
                new BusinessChannel("ch-" + invocation.getArgument(1), "biz-" + invocation.getArgument(1),
                        invocation.getArgument(0), invocation.getArgument(1), true));
        var accounts = mock(BusinessAccountRepository.class);
        when(accounts.findByBusinessId(anyString())).thenAnswer(invocation ->
                new BusinessAccount(invocation.getArgument(0), profile.businessName(), profile));
        requests = mock(BusinessRequestService.class);
        service = new ConversationService(repository, orchestrator, ai, slots, accounts, channels,
                availability, requests, boundary);
    }

    private InquiryResponse say(String message) {
        return service.process("customer", BusinessChannelType.MESSENGER, "100", message);
    }

    private void start() {
        when(ai.analyzeRequest("I need a hotel in Paris", profile)).thenReturn(
                new RequestAnalysis("ROOM_BOOKING", 0.99, Map.of("location", "Paris")));
        say("I need a hotel in Paris");
    }

    private void followUp(String message, Map<String, Object> entities) {
        when(ai.analyzeConversationIntent(eq(profile), eq("ROOM_BOOKING"), anyMap(), anyList(), eq(message)))
                .thenReturn(new ConversationIntentAnalysis("FOLLOW_UP", .99));
        when(ai.analyzeFollowUp(eq("ROOM_BOOKING"), anyMap(), anyList(), eq(message)))
                .thenReturn(new FollowUpAnalysis(entities));
    }

    @Test void accumulatesMultipleFieldsAndCreatesOnePendingRequest() {
        start();
        followUp("Next Friday for two adults", Map.of("checkInDate", "next Friday", "guestCount", 2));
        var result = say("Next Friday for two adults");
        assertEquals("Paris", result.inquiry().fields().get("location"));
        assertEquals(2, result.inquiry().fields().get("guestCount"));
        assertEquals(List.of("durationNights"), result.missingFields());
        assertEquals("How many nights?", result.reply());
        followUp("Three nights", Map.of("durationNights", 3));
        var completed = say("Three nights");
        assertEquals(InquiryStatus.INFORMATION_COLLECTED, completed.status());
        verify(requests).create(eq("biz-100"), eq(new ConversationIdentity("biz-100", BusinessChannelType.MESSENGER,
                "100", "customer").sessionId()), eq("ROOM_BOOKING"),
                argThat(fields -> fields.get("guestCount").equals(2) && fields.get("durationNights").equals(3)),
                eq(AvailabilityStatus.INDICATED));
        assertTrue(sessions.isEmpty());
    }

    @Test void explicitCorrectionsWin() {
        start();
        followUp("Next Friday for two adults", Map.of("checkInDate", "next Friday", "guestCount", 2));
        say("Next Friday for two adults");
        followUp("Actually make it three adults", Map.of("guestCount", 3));
        var result = say("Actually make it three adults");
        assertEquals(3, result.inquiry().fields().get("guestCount"));
        assertEquals("next Friday", result.inquiry().fields().get("checkInDate"));
        assertEquals(List.of("durationNights"), result.missingFields());
    }

    @Test void businessQuestionPreservesUnfinishedRequest() {
        start();
        when(ai.analyzeConversationIntent(eq(profile), eq("ROOM_BOOKING"), anyMap(), anyList(), eq("Do you have free parking?")))
                .thenReturn(new ConversationIntentAnalysis("NEW_REQUEST", .99));
        when(ai.analyzeRequest("Do you have free parking?", profile))
                .thenReturn(new RequestAnalysis("BUSINESS_QUESTION", .99, Map.of()));
        when(ai.answerBusinessQuestion("Do you have free parking?", profile)).thenReturn("Yes, parking is free.");
        assertEquals("Yes, parking is free.", say("Do you have free parking?").reply());
        assertEquals(1, sessions.size());
        assertEquals("Paris", sessions.values().iterator().next().getInquiry().fields().get("location"));
    }

    @Test void failedNewAnalysisPreservesPreviousState() {
        start();
        when(ai.analyzeConversationIntent(eq(profile), anyString(), anyMap(), anyList(), eq("Another request")))
                .thenReturn(new ConversationIntentAnalysis("NEW_REQUEST", .99));
        when(ai.analyzeRequest("Another request", profile)).thenThrow(new IllegalStateException("AI unavailable"));
        assertThrows(IllegalStateException.class, () -> say("Another request"));
        assertEquals(1, sessions.size());
    }

    @Test void sameCustomerNeverSharesConversationAcrossPagesOrChannels() {
        start();
        when(ai.analyzeRequest("Hello", profile)).thenReturn(new RequestAnalysis("GREETING", .99, Map.of()));
        service.process("customer", BusinessChannelType.MESSENGER, "101", "Hello");
        service.process("customer", BusinessChannelType.WEBSITE, "100", "Hello");
        verify(ai, never()).analyzeConversationIntent(any(BusinessProfile.class), anyString(), anyMap(), anyList(), anyString());
        assertEquals(1, sessions.size());
    }

    @Test void rejectsDisabledChannelBeforeAi() {
        when(channels.findByTypeAndExternalId(BusinessChannelType.MESSENGER, "100"))
                .thenReturn(new BusinessChannel("ch", "biz", BusinessChannelType.MESSENGER, "100", false));
        assertThrows(IllegalStateException.class, () -> say("hi"));
        verifyNoInteractions(ai);
    }

    @Test void unknownRequestAsksForClarificationEvenWithHighConfidence() {
        when(ai.analyzeRequest("Can you help?", profile))
                .thenReturn(new RequestAnalysis("UNKNOWN", .99, Map.of()));
        assertEquals(InquiryStatus.NEEDS_CLARIFICATION, say("Can you help?").status());
        verifyNoInteractions(requests);
    }

    @Test void mergeDoesNotEraseUsefulValuesOrMutateInputs() {
        var existing = Map.<String, Object>of("guestCount", 2, "location", "Paris");
        var incoming = new HashMap<String, Object>();
        incoming.put("guestCount", 3);
        incoming.put("location", null);
        var merged = EntityMerger.merge(existing, incoming);
        assertEquals(Map.of("guestCount", 3, "location", "Paris"), merged);
        assertEquals(2, existing.get("guestCount"));
        assertEquals(existing, EntityMerger.merge(existing, null));
    }

    @Test void identityRoundTripsWithoutDelimiterCollisions() {
        var identity = new ConversationIdentity("business.a", BusinessChannelType.WEBSITE, "site:x", "customer.y");
        assertEquals(identity, ConversationIdentity.fromSessionId(identity.sessionId()));
        assertNull(ConversationIdentity.fromSessionId("old-legacy-session"));
        assertNull(ConversationIdentity.fromSessionId("v1.bad"));
    }

    @Test void initialMixedRequestAnswersKnowledgeAndRetainsBookingEntities() {
        String message = "I need a room in Paris next Friday for 2 people and do you have parking?";
        when(ai.analyzeRequest(message, profile)).thenReturn(new RequestAnalysis("ROOM_BOOKING", .99,
                Map.of("location", "Paris", "checkInDate", "next Friday", "guestCount", 2), List.of("Do you have parking?")));
        when(ai.answerBusinessQuestion("Do you have parking?", profile)).thenReturn("Parking is free.");
        var reply = say(message);
        assertTrue(reply.reply().startsWith("Parking is free."));
        assertEquals(List.of("durationNights"), reply.missingFields());
        assertEquals(2, reply.inquiry().fields().get("guestCount"));
        assertEquals(1, sessions.size());
        followUp("Three nights", Map.of("durationNights", 3));
        say("Three nights");
        verify(requests).create(anyString(), anyString(), eq("ROOM_BOOKING"),
                argThat(fields -> fields.get("durationNights").equals(3) && fields.get("guestCount").equals(2)), any());
    }

    @Test void explicitKnowledgeInterruptionRetainsMissingFieldsAndResumes() {
        start();
        when(ai.analyzeConversationIntent(eq(profile), anyString(), anyMap(), anyList(), eq("Parking?")))
                .thenReturn(new ConversationIntentAnalysis("BUSINESS_QUESTION", .99, List.of("Is parking free?")));
        when(ai.answerBusinessQuestion("Is parking free?", profile)).thenReturn("Parking is free.");
        var reply = say("Parking?");
        assertEquals("Parking is free.", reply.reply());
        assertEquals(List.of("checkInDate", "guestCount", "durationNights"), reply.missingFields());
        assertEquals("Paris", reply.inquiry().fields().get("location"));
        followUp("Next Friday for two adults", Map.of("checkInDate", "next Friday", "guestCount", 2));
        assertEquals(List.of("durationNights"), say("Next Friday for two adults").missingFields());
    }

    @Test void completingMixedFollowUpKeepsKnowledgeAnswerAndCreatesOnlyOneRequest() {
        start();
        String message = "Next Friday for two adults for three nights. Is parking free?";
        followUp(message, Map.of("checkInDate", "next Friday", "guestCount", 2, "durationNights", 3));
        when(ai.analyzeConversationIntent(eq(profile), anyString(), anyMap(), anyList(), eq(message)))
                .thenReturn(new ConversationIntentAnalysis("FOLLOW_UP", .99, List.of("Is parking free?")));
        when(ai.answerBusinessQuestion("Is parking free?", profile)).thenReturn("Parking is free.");
        var reply = say(message);
        assertTrue(reply.reply().startsWith("Parking is free."));
        assertEquals(InquiryStatus.INFORMATION_COLLECTED, reply.status());
        assertTrue(sessions.isEmpty());
        verify(requests, times(1)).create(anyString(), anyString(), eq("ROOM_BOOKING"), anyMap(), any());
    }

    @Test void fullySpecifiedInitialMixedRequestKeepsBothAnswerAndReceipt() {
        String message = "Room in Paris next Friday for two guests for three nights and is parking free?";
        when(ai.analyzeRequest(message, profile)).thenReturn(new RequestAnalysis("ROOM_BOOKING", .99,
                Map.of("location", "Paris", "checkInDate", "next Friday", "guestCount", 2, "durationNights", 3),
                List.of("Is parking free?")));
        when(ai.answerBusinessQuestion("Is parking free?", profile)).thenReturn("Parking is free.");
        var reply = say(message);
        assertTrue(reply.reply().startsWith("Parking is free."));
        assertTrue(reply.reply().contains("confirm availability"));
        verify(requests, times(1)).create(anyString(), anyString(), eq("ROOM_BOOKING"), anyMap(), any());
    }

    @Test void failedKnowledgeAnswerDoesNotDiscardMixedWorkflow() {
        String message = "A room in Paris and parking?";
        when(ai.analyzeRequest(message, profile)).thenReturn(new RequestAnalysis("ROOM_BOOKING", .99,
                Map.of("location", "Paris"), List.of("Is parking free?")));
        when(ai.answerBusinessQuestion("Is parking free?", profile)).thenThrow(new IllegalStateException("upstream secret"));
        var reply = say(message);
        assertFalse(reply.reply().contains("secret"));
        assertTrue(reply.reply().contains("try again"));
        assertEquals(1, sessions.size());
        assertEquals("Paris", reply.inquiry().fields().get("location"));
    }
}
