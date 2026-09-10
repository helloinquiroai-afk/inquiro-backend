package com.inquiro.communication.messenger;

import com.inquiro.config.MessengerProperties;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Transport only. A token is used exclusively for its configured Page. */
@Service
public class MessengerSendService {
    private final MessengerProperties properties;
    private final RestClient client;

    @org.springframework.beans.factory.annotation.Autowired
    public MessengerSendService(MessengerProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        this.client = builder.baseUrl("https://graph.facebook.com").requestFactory(factory).build();
    }

    MessengerSendService(MessengerProperties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    public boolean supportsPage(String pageId) {
        return pageId != null && pageId.matches("[0-9]{1,64}") && pageId.equals(properties.getPageId())
                && properties.getPageAccessToken() != null && !properties.getPageAccessToken().isBlank();
    }

    public void sendText(String recipientId, String text) {
        sendText(properties.getPageId(), recipientId, text);
    }

    public void sendText(String pageId, String recipientId, String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Reply must not be empty");
        int end = text.offsetByCodePoints(0, Math.min(2000, text.codePointCount(0, text.length())));
        post(pageId, recipientId, Map.of("recipient", Map.of("id", recipientId),
                "messaging_type", "RESPONSE", "message", Map.of("text", text.substring(0, end))));
    }

    public void sendAction(String pageId, String recipientId, String action) {
        if (!java.util.Set.of("mark_seen", "typing_on", "typing_off").contains(action)) {
            throw new IllegalArgumentException("Unsupported sender action");
        }
        post(pageId, recipientId, Map.of("recipient", Map.of("id", recipientId), "sender_action", action));
    }

    private void post(String pageId, String recipientId, Map<String, Object> body) {
        if (!supportsPage(pageId)) throw new IllegalStateException("Messenger Page credentials are not configured");
        if (recipientId == null || !recipientId.matches("[0-9]{1,64}")) {
            throw new IllegalArgumentException("Invalid Messenger recipient");
        }
        if (!properties.getGraphApiVersion().matches("v[0-9]+\\.[0-9]+")) {
            throw new IllegalStateException("Invalid Graph API version configuration");
        }
        client.post().uri("/{version}/{page}/messages", properties.getGraphApiVersion(), pageId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getPageAccessToken())
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
    }
}
