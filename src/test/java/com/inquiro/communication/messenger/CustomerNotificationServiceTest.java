package com.inquiro.communication.messenger;

import com.inquiro.availability.AvailabilityStatus;
import com.inquiro.business.*;
import com.inquiro.config.MessengerProperties;
import com.inquiro.conversation.ConversationIdentity;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerNotificationServiceTest {
    @Test void notificationsUseOriginalPageAndRawRecipientOnly() {
        var sender = mock(MessengerSendService.class);
        var channels = mock(BusinessChannelRepository.class);
        when(channels.findByTypeAndExternalId(BusinessChannelType.MESSENGER, "100"))
                .thenReturn(new BusinessChannel("channel", "biz", BusinessChannelType.MESSENGER, "100", true));
        var service = new CustomerNotificationService(sender, channels, new MessengerProperties());
        var identity = new ConversationIdentity("biz", BusinessChannelType.MESSENGER, "100", "200");
        service.sendConfirmation(request("biz", identity.sessionId()));
        verify(sender).sendText(eq("100"), eq("200"), contains("confirmed"));
    }

    @Test void neverSendsWebsiteOrOtherBusinessRequestsToMessenger() {
        var sender = mock(MessengerSendService.class);
        var service = new CustomerNotificationService(sender, mock(BusinessChannelRepository.class), new MessengerProperties());
        service.sendConfirmation(request("biz", new ConversationIdentity("biz", BusinessChannelType.WEBSITE, "site", "customer").sessionId()));
        service.sendRejection(request("other", new ConversationIdentity("biz", BusinessChannelType.MESSENGER, "100", "200").sessionId()));
        verifyNoInteractions(sender);
    }

    private BusinessRequest request(String business, String customer) {
        return new BusinessRequest("request", business, customer, "ROOM_BOOKING", Map.of(),
                AvailabilityStatus.INDICATED, BusinessRequestStatus.CONFIRMED, Instant.now());
    }
}