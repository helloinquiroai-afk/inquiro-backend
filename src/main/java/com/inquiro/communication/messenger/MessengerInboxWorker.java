package com.inquiro.communication.messenger;

import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessChannelType;
import com.inquiro.config.MessengerProperties;
import com.inquiro.conversation.ConversationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessengerInboxWorker {
    static final String FRIENDLY_FAILURE = "I'm sorry, I'm having trouble answering right now. Please try again shortly, or contact the business directly.";
    private final MessengerInboxRepository inbox;
    private final ConversationService conversations;
    private final BusinessChannelRepository channels;
    private final MessengerSendService sender;
    private final MessengerProperties properties;

    /** Conversation changes and the reply commit together, before any reply send is attempted. */
    @Transactional
    public void prepare(Long id) {
        MessengerInboxEvent event = inbox.lockById(id).orElseThrow();
        if (event.getStatus() != MessengerInboxEvent.Status.RECEIVED) return;
        if (!validChannel(event)) {
            fail(event, "CHANNEL_UNAVAILABLE");
            return;
        }
        action(event, "mark_seen");
        action(event, "typing_on");
        String reply;
        try {
            var response = conversations.process(event.getSenderId(), BusinessChannelType.MESSENGER,
                    event.getPageId(), event.getMessageText());
            reply = response == null ? null : response.reply();
            if (reply == null || reply.isBlank()) reply = FRIENDLY_FAILURE;
        } catch (RuntimeException exception) {
            log.warn("event=messenger_ai_failure business_id={} inbox_id={} error_type={}",
                    event.getBusinessId(), id, exception.getClass().getSimpleName());
            reply = FRIENDLY_FAILURE;
        } finally {
            action(event, "typing_off");
        }
        event.setReply(reply.length() > 10000 ? reply.substring(0, 10000) : reply);
        event.setStatus(MessengerInboxEvent.Status.REPLY_READY);
        event.setNextAttemptAt(Instant.now());
    }

    @Transactional
    public void deliver(Long id) {
        MessengerInboxEvent event = inbox.lockById(id).orElseThrow();
        if (event.getStatus() != MessengerInboxEvent.Status.REPLY_READY
                || event.getNextAttemptAt().isAfter(Instant.now())) return;
        if (!validChannel(event)) {
            fail(event, "CHANNEL_UNAVAILABLE");
            return;
        }
        event.setSendAttempts(event.getSendAttempts() + 1);
        try {
            sender.sendText(event.getPageId(), event.getSenderId(), event.getReply());
            event.setStatus(MessengerInboxEvent.Status.SENT);
            event.setFailureCode(null);
            log.info("event=messenger_reply_sent business_id={} inbox_id={}", event.getBusinessId(), id);
        } catch (RuntimeException exception) {
            boolean permanent = exception instanceof RestClientResponseException http
                    && http.getStatusCode().is4xxClientError()
                    && http.getStatusCode().value() != 429 && http.getStatusCode().value() != 408;
            String code = exception instanceof RestClientResponseException http
                    ? "META_HTTP_" + http.getStatusCode().value() : exception.getClass().getSimpleName();
            event.setFailureCode(code);
            if (permanent || event.getSendAttempts() >= properties.getMaxSendAttempts()) {
                fail(event, code);
            } else {
                event.setNextAttemptAt(Instant.now().plusSeconds(Math.min(300, 5L << event.getSendAttempts())));
                log.warn("event=messenger_send_retry business_id={} inbox_id={} attempt={} error_code={}",
                        event.getBusinessId(), id, event.getSendAttempts(), code);
            }
        }
    }

    private boolean validChannel(MessengerInboxEvent event) {
        var channel = channels.findByTypeAndExternalId(BusinessChannelType.MESSENGER, event.getPageId());
        return channel != null && channel.enabled() && channel.businessId().equals(event.getBusinessId())
                && sender.supportsPage(event.getPageId());
    }

    private void action(MessengerInboxEvent event, String action) {
        try {
            sender.sendAction(event.getPageId(), event.getSenderId(), action);
        } catch (RuntimeException exception) {
            log.debug("event=messenger_action_failed inbox_id={} action={} error_type={}",
                    event.getId(), action, exception.getClass().getSimpleName());
        }
    }

    private void fail(MessengerInboxEvent event, String code) {
        event.setStatus(MessengerInboxEvent.Status.FAILED);
        event.setFailureCode(code);
        log.error("event=messenger_failed business_id={} inbox_id={} error_code={}",
                event.getBusinessId(), event.getId(), code);
    }
}
