package com.inquiro.channel;

import com.inquiro.business.*;
import com.inquiro.conversation.ConversationService;
import com.inquiro.inquiry.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChannelMessageRouterTest {

    @Test
    void routesMessageToBusinessAndBuildsStableSessionIdentity() {
        BusinessChannelRepository channels = mock(BusinessChannelRepository.class);
        BusinessAccountRepository accounts = mock(BusinessAccountRepository.class);
        ConversationService conversations = mock(ConversationService.class);

        BusinessChannel channel = new BusinessChannel(
                "channel-1", "biz-1", BusinessChannelType.WEBSITE, "site-1", true);
        BusinessAccount account = new BusinessAccount(
                "biz-1", "Test Business", null);

        when(channels.findByTypeAndExternalId(BusinessChannelType.WEBSITE, "site-1"))
                .thenReturn(channel);
        when(accounts.findByBusinessId("biz-1")).thenReturn(account);

        InquiryResponse response = new InquiryResponse(
                new InquiryResult("HOSPITALITY", "ROOM_BOOKING", java.util.Map.of()),
                List.of("checkInDate"),
                InquiryStatus.NEEDS_INFORMATION,
                "Please provide your dates.");

        when(conversations.process(
                "customer-1",
                BusinessChannelType.WEBSITE,
                "site-1",
                "I need a room"))
                .thenReturn(response);

        ChannelMessageResult result = new ChannelMessageRouter(
                channels, accounts, conversations).route(
                        new ChannelMessage(
                                BusinessChannelType.WEBSITE,
                                "site-1",
                                "customer-1",
                                "I need a room"));

        assertEquals("biz-1", result.businessId());
        assertEquals(
                "v1.Yml6LTE.WEBSITE.c2l0ZS0x.Y3VzdG9tZXItMQ",
                result.sessionId());
        assertSame(response, result.response());
        verify(conversations).process(
                "customer-1",
                BusinessChannelType.WEBSITE,
                "site-1",
                "I need a room");
    }

    @Test
    void rejectsDisabledChannelBeforeConversationProcessing() {
        BusinessChannelRepository channels = mock(BusinessChannelRepository.class);
        BusinessAccountRepository accounts = mock(BusinessAccountRepository.class);
        ConversationService conversations = mock(ConversationService.class);

        when(channels.findByTypeAndExternalId(BusinessChannelType.WEBSITE, "site-1"))
                .thenReturn(new BusinessChannel(
                        "channel-1", "biz-1", BusinessChannelType.WEBSITE, "site-1", false));

        assertThrows(IllegalStateException.class, () ->
                new ChannelMessageRouter(channels, accounts, conversations).route(
                        new ChannelMessage(
                                BusinessChannelType.WEBSITE,
                                "site-1",
                                "customer-1",
                                "hello")));

        verifyNoInteractions(conversations);
    }

    @Test
    void rejectsUnknownChannel() {
        BusinessChannelRepository channels = mock(BusinessChannelRepository.class);
        when(channels.findByTypeAndExternalId(BusinessChannelType.MESSENGER, "page-1"))
                .thenReturn(null);

        assertThrows(IllegalStateException.class, () ->
                new ChannelMessageRouter(
                        channels,
                        mock(BusinessAccountRepository.class),
                        mock(ConversationService.class)).route(
                        new ChannelMessage(
                                BusinessChannelType.MESSENGER,
                                "page-1",
                                "customer-1",
                                "hello")));
    }
}
