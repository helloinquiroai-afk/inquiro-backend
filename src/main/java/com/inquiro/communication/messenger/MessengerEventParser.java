package com.inquiro.communication.messenger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessengerEventParser {
    private final ObjectMapper mapper;

    public List<MessengerEvent> parse(byte[] payload) throws IOException {
        JsonNode root = mapper.readTree(payload);
        if (root == null || !root.isObject()) throw new IOException("Expected a webhook object");
        List<MessengerEvent> events = new ArrayList<>();
        if (!"page".equals(root.path("object").asText())) return events;
        for (JsonNode entry : root.path("entry")) {
            for (JsonNode event : entry.path("messaging")) {
                JsonNode message = event.path("message");
                if (message.path("is_echo").asBoolean(false) || !message.path("text").isTextual()) continue;
                String sender = event.path("sender").path("id").asText("");
                String page = event.path("recipient").path("id").asText("");
                String text = message.path("text").asText("");
                String id = message.path("mid").asText("");
                long timestamp = event.path("timestamp").asLong(0);
                if (!sender.matches("[0-9]{1,64}") || !page.matches("[0-9]{1,64}")
                        || sender.equals(page) || text.isBlank() || text.length() > 10000
                        || id.length() > 512) continue;
                if (!entry.path("id").asText(page).equals(page)) continue;
                events.add(new MessengerEvent(sender, page, text, id, timestamp));
            }
        }
        return List.copyOf(events);
    }
}