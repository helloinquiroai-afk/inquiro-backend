package com.inquiro.communication.messenger;

import com.inquiro.config.MessengerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MessengerSendServiceTest {
    private MessengerSendService sender;
    private MockRestServiceServer server;

    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("https://graph.facebook.com");
        server = MockRestServiceServer.bindTo(builder).build();
        var properties = new MessengerProperties();
        properties.setPageId("100");
        properties.setPageAccessToken("test-page-token");
        properties.setGraphApiVersion("v26.0");
        sender = new MessengerSendService(properties, builder.build());
    }

    @Test void sendsPageScopedReplyWithBearerTokenAndResponseType() {
        server.expect(requestTo("https://graph.facebook.com/v26.0/100/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-page-token"))
                .andExpect(content().json("""
                        {"recipient":{"id":"200"},"messaging_type":"RESPONSE","message":{"text":"Hello"}}
                        """)).andRespond(withSuccess());
        sender.sendText("100", "200", "Hello");
        server.verify();
    }

    @Test void sendsSupportedActions() {
        for (String action : new String[]{"mark_seen", "typing_on", "typing_off"}) {
            server.expect(requestTo("https://graph.facebook.com/v26.0/100/messages"))
                    .andExpect(content().json("{\"recipient\":{\"id\":\"200\"},\"sender_action\":\"" + action + "\"}"))
                    .andRespond(withSuccess());
        }
        for (String action : new String[]{"mark_seen", "typing_on", "typing_off"}) sender.sendAction("100", "200", action);
        server.verify();
    }

    @Test void neverUsesOnePagesTokenForAnotherPage() {
        assertFalse(sender.supportsPage("101"));
        assertThrows(IllegalStateException.class, () -> sender.sendText("101", "200", "Hello"));
        assertThrows(IllegalArgumentException.class, () -> sender.sendText("100", "../wrong", "Hello"));
        server.verify();
    }

    @Test void returnsMetaErrorsToRetryHandler() {
        server.expect(requestTo("https://graph.facebook.com/v26.0/100/messages")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThrows(org.springframework.web.client.RestClientResponseException.class, () -> sender.sendText("100", "200", "Hello"));
    }
}