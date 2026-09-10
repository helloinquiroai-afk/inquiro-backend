package com.inquiro.communication.messenger;

import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessChannelType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessengerMessageProcessor {
    private final MessengerEventParser parser;
    private final BusinessChannelRepository channels;
    private final MessengerInboxRepository inbox;
    private final MessengerInboxStore store;
    private final MessengerSendService sender;

    public void process(String payload) throws IOException {
        process(payload.getBytes(StandardCharsets.UTF_8));
    }

    public void process(byte[] payload) throws IOException {
        for (MessengerEvent event : parser.parse(payload)) {
            var channel = channels.findByTypeAndExternalId(BusinessChannelType.MESSENGER, event.pageId());
            if (channel == null || !channel.enabled() || !sender.supportsPage(event.pageId())) {
                log.warn("event=messenger_unconfigured_page");
                continue;
            }
            String key = eventKey(event);
            if (inbox.existsByEventKey(key)) continue;
            try {
                store.insert(new MessengerInboxEvent(key, channel.businessId(), event));
            } catch (DataIntegrityViolationException exception) {
                // The unique constraint handles concurrent redelivery, including across restarts.
                if (!inbox.existsByEventKey(key)) throw exception;
            }
        }
    }

    static String eventKey(MessengerEvent event) {
        String identity = !event.messageId().isBlank() ? event.messageId()
                : event.timestamp() > 0 ? event.senderId() + ":" + event.timestamp() + ":" + event.text()
                : UUID.randomUUID().toString();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((event.pageId() + ":" + identity).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}