package com.inquiro.communication.messenger;

import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessChannelType;
import com.inquiro.business.BusinessRequest;
import com.inquiro.config.MessengerProperties;
import com.inquiro.conversation.ConversationIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerNotificationService {
    private final MessengerSendService messengerSendService;
    private final BusinessChannelRepository channels;
    private final MessengerProperties properties;

    public void sendConfirmation(BusinessRequest request) {
        send(request, "Your " + formatService(request.service())
                + " request has been confirmed by the business. Thank you for choosing us.");
    }

    public void sendRejection(BusinessRequest request) {
        send(request, "Unfortunately, your " + formatService(request.service())
                + " request could not be confirmed. Please contact the business for an alternative.");
    }

    private void send(BusinessRequest request, String message) {
        ConversationIdentity identity = ConversationIdentity.fromSessionId(request.customerId());
        if (identity != null && (identity.channel() != BusinessChannelType.MESSENGER
                || !identity.businessId().equals(request.businessId()))) return;
        String page = identity == null ? properties.getPageId() : identity.externalId();
        String recipient = identity == null ? request.customerId() : identity.customerId();
        var channel = channels.findByTypeAndExternalId(BusinessChannelType.MESSENGER, page);
        if (channel == null || !channel.enabled() || !channel.businessId().equals(request.businessId())) return;
        try {
            messengerSendService.sendText(page, recipient, message);
        } catch (RuntimeException exception) {
            // Booking state remains authoritative if a notification cannot be delivered.
            log.warn("event=business_notification_failed business_id={} request_id={} error_type={}",
                    request.businessId(), request.requestId(), exception.getClass().getSimpleName());
        }
    }

    private String formatService(String service) {
        return service == null || service.isBlank() ? "service" : service.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }
}