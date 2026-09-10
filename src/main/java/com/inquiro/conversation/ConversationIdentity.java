package com.inquiro.conversation;

import com.inquiro.business.BusinessChannelType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Stable, reversible identity used by persisted conversations and request notification routing. */
public record ConversationIdentity(String businessId, BusinessChannelType channel, String externalId, String customerId) {
    public String sessionId() {
        String key = "v1." + encode(businessId) + "." + channel.name() + "." + encode(externalId) + "." + encode(customerId);
        if (key.length() > 255) throw new IllegalArgumentException("Conversation identifier is too long");
        return key;
    }

    public static ConversationIdentity fromSessionId(String key) {
        if (key == null || !key.startsWith("v1.")) return null;
        try {
            String[] parts = key.split("\\.", -1);
            if (parts.length != 5) return null;
            return new ConversationIdentity(decode(parts[1]), BusinessChannelType.valueOf(parts[2]),
                    decode(parts[3]), decode(parts[4]));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String encode(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Conversation identity is required");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
