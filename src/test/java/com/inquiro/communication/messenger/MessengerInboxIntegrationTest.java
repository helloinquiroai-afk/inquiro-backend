package com.inquiro.communication.messenger;

import com.inquiro.ai.*;
import com.inquiro.business.*;
import com.inquiro.conversation.ConversationSessionJpaRepository;
import com.inquiro.request.RequestDefinition;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MessengerInboxIntegrationTest {
    @Autowired MessengerMessageProcessor processor;
    @Autowired MessengerInboxRepository inbox;
    @Autowired MessengerInboxWorker worker;
    @Autowired BusinessAccountRepository accounts;
    @Autowired BusinessChannelRepository channels;
    @Autowired ConversationSessionJpaRepository sessions;
    @Autowired BusinessRequestJpaRepository requests;
    @Autowired MessengerOperationsController operations;
    @MockitoBean AiService ai;
    @MockitoBean MessengerSendService sender;

    @BeforeEach void setup() {
        inbox.deleteAll();
        sessions.deleteAll();
        requests.deleteAll();
        var profile = new BusinessProfile("Hotel", "HOSPITALITY", "Hotel", List.of(
                new RequestDefinition("ROOM_BOOKING", "Room booking", List.of("location", "checkInDate"))),
                new BusinessKnowledge("Hotel", List.of("ROOM_BOOKING"), List.of(), Map.of(), List.of(), List.of(), ""));
        accounts.save(new BusinessAccount("biz-100", "Hotel", profile));
        channels.save(new BusinessChannel("ch-100", "biz-100", BusinessChannelType.MESSENGER, "100", true));
        when(sender.supportsPage("100")).thenReturn(true);
    }

    private byte[] payload(String mid) {
        return ("""
                {"object":"page","entry":[{"id":"100","messaging":[
                  {"sender":{"id":"200"},"recipient":{"id":"100"},"timestamp":123,
                   "message":{"mid":"%s","text":"I need a room in Paris"}}]}]}
                """.formatted(mid)).getBytes(StandardCharsets.UTF_8);
    }

    @Test void acknowledgesPersistedMessageWithoutCallingAiThenDeduplicates() throws Exception {
        processor.process(payload("one"));
        processor.process(payload("one"));
        assertEquals(1, inbox.count());
        verifyNoInteractions(ai);
        assertEquals("biz-100", inbox.findAll().get(0).getBusinessId());
    }

    @Test void databaseConstraintProtectsConcurrentDuplicates() throws Exception {
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            var first = executor.submit(() -> { start.await(); processor.process(payload("race")); return true; });
            var second = executor.submit(() -> { start.await(); processor.process(payload("race")); return true; });
            start.countDown();
            assertTrue(first.get());
            assertTrue(second.get());
            assertEquals(1, inbox.count());
        } finally { executor.shutdownNow(); }
    }

    @Test void retryUsesPersistedReplyAndNeverRepeatsConversationProcessing() throws Exception {
        when(ai.analyzeRequest(anyString(), any())).thenReturn(new RequestAnalysis("ROOM_BOOKING", .99, Map.of("location", "Paris")));
        processor.process(payload("retry"));
        long id = inbox.findAll().get(0).getId();
        worker.prepare(id);
        assertEquals(MessengerInboxEvent.Status.REPLY_READY, inbox.findById(id).orElseThrow().getStatus());
        assertEquals(1, sessions.count());
        doThrow(new ResourceAccessException("timeout")).doNothing().when(sender).sendText(eq("100"), eq("200"), anyString());
        worker.deliver(id);
        var event = inbox.findById(id).orElseThrow();
        assertEquals(MessengerInboxEvent.Status.REPLY_READY, event.getStatus());
        assertEquals(1, event.getSendAttempts());
        event.setNextAttemptAt(Instant.now().minusSeconds(1));
        inbox.saveAndFlush(event);
        worker.prepare(id);
        worker.deliver(id);
        worker.deliver(id);
        assertEquals(MessengerInboxEvent.Status.SENT, inbox.findById(id).orElseThrow().getStatus());
        verify(ai, times(1)).analyzeRequest(anyString(), any());
        verify(sender, times(2)).sendText(eq("100"), eq("200"), anyString());
    }

    @Test void friendlyAiFailureAndOptionalIndicatorsStillSendReply() throws Exception {
        when(ai.analyzeRequest(anyString(), any())).thenThrow(new IllegalStateException("401 secret error"));
        doThrow(new ResourceAccessException("offline")).when(sender).sendAction(anyString(), anyString(), anyString());
        processor.process(payload("failure"));
        long id = inbox.findAll().get(0).getId();
        worker.prepare(id);
        worker.deliver(id);
        verify(sender).sendText("100", "200", MessengerInboxWorker.FRIENDLY_FAILURE);
        assertEquals(MessengerInboxEvent.Status.SENT, inbox.findById(id).orElseThrow().getStatus());
    }

    @Test void pageReassignmentCannotConsumeAnotherBusinessEvent() throws Exception {
        processor.process(payload("reassigned"));
        long id = inbox.findAll().get(0).getId();
        channels.save(new BusinessChannel("ch-100", "different-business", BusinessChannelType.MESSENGER, "100", true));
        worker.prepare(id);
        worker.deliver(id);
        assertEquals(MessengerInboxEvent.Status.FAILED, inbox.findById(id).orElseThrow().getStatus());
        verifyNoInteractions(ai);
        verify(sender, never()).sendText(anyString(), anyString(), anyString());
    }

    @Test void successfulDeliverySendsActionsInOrder() throws Exception {
        when(ai.analyzeRequest(anyString(), any())).thenReturn(new RequestAnalysis("GREETING", .99, Map.of()));
        processor.process(payload("actions"));
        long id = inbox.findAll().get(0).getId();
        worker.prepare(id);
        worker.deliver(id);
        var order = inOrder(sender, ai);
        order.verify(sender).sendAction("100", "200", "mark_seen");
        order.verify(sender).sendAction("100", "200", "typing_on");
        order.verify(ai).analyzeRequest(anyString(), any());
        order.verify(sender).sendAction("100", "200", "typing_off");
        order.verify(sender).sendText(eq("100"), eq("200"), anyString());
    }

    @Test void failedDeliveryCanBeRetriedOnlyWithinItsBusiness() throws Exception {
        when(ai.analyzeRequest(anyString(), any())).thenReturn(new RequestAnalysis("GREETING", .99, Map.of()));
        processor.process(payload("operator-retry"));
        long id = inbox.findAll().get(0).getId();
        worker.prepare(id);
        var event = inbox.findById(id).orElseThrow();
        event.setStatus(MessengerInboxEvent.Status.FAILED);
        event.setFailureCode("META_HTTP_401");
        inbox.saveAndFlush(event);
        assertTrue(operations.list("other-business", MessengerInboxEvent.Status.FAILED).isEmpty());
        assertEquals(1, operations.list("biz-100", MessengerInboxEvent.Status.FAILED).size());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> operations.retry("other-business", id));
        assertEquals(MessengerInboxEvent.Status.REPLY_READY, operations.retry("biz-100", id).status());
        worker.prepare(id);
        worker.deliver(id);
        verify(ai, times(1)).analyzeRequest(anyString(), any());
        assertEquals(MessengerInboxEvent.Status.SENT, inbox.findById(id).orElseThrow().getStatus());
    }
}
