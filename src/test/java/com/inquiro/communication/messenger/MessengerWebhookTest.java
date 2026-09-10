package com.inquiro.communication.messenger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.communication.common.MetaWebhookController;
import com.inquiro.communication.whatsapp.WhatsAppMessageProcessor;
import com.inquiro.communication.whatsapp.WhatsAppWebhookController;
import com.inquiro.config.MessengerProperties;
import com.inquiro.config.WhatsAppProperties;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessengerWebhookTest {
    private MockMvc mvc;
    private MessengerMessageProcessor processor;
    private WhatsAppMessageProcessor whatsApp;
    private MessengerProperties properties;

    @BeforeEach void setup() {
        processor = mock(MessengerMessageProcessor.class);
        whatsApp = mock(WhatsAppMessageProcessor.class);
        properties = new MessengerProperties();
        properties.setVerifyToken("test-verify");
        properties.setAppSecret("test-app-secret");
        var waProperties = new WhatsAppProperties();
        waProperties.setVerifyToken("wa-verify");
        waProperties.setAppSecret("wa-secret");
        var signatures = new MetaSignatureValidator();
        var messengerController = new MessengerWebhookController(properties, processor, signatures);
        var waController = new WhatsAppWebhookController(waProperties, whatsApp, signatures);
        var shared = new MetaWebhookController(messengerController, waController, properties, waProperties,
                signatures, new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(shared, messengerController, waController).build();
    }

    static String sign(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test void verifiesBothMessengerUrls() throws Exception {
        for (String path : new String[]{"/webhook", "/messenger/webhook"}) {
            mvc.perform(get(path).param("hub.mode", "subscribe").param("hub.verify_token", "test-verify")
                    .param("hub.challenge", "00123")).andExpect(status().isOk()).andExpect(content().string("00123"));
        }
    }

    @Test void rejectsMissingInvalidAndEmptyVerificationTokens() throws Exception {
        mvc.perform(get("/webhook")).andExpect(status().isForbidden());
        mvc.perform(get("/webhook").param("hub.mode", "unsubscribe").param("hub.verify_token", "test-verify")
                .param("hub.challenge", "1")).andExpect(status().isForbidden());
        properties.setVerifyToken("");
        mvc.perform(get("/messenger/webhook").param("hub.mode", "subscribe").param("hub.verify_token", "")
                .param("hub.challenge", "1")).andExpect(status().isForbidden());
    }

    @Test void acceptsRawSignedUnicodeBodyWithoutChangingBytes() throws Exception {
        byte[] body = "{\"object\":\"page\",\"text\":\"ආයුබෝවන්\"}".getBytes(StandardCharsets.UTF_8);
        mvc.perform(post("/webhook").contentType("application/json").content(body)
                .header("X-Hub-Signature-256", sign(body, "test-app-secret"))).andExpect(status().isOk());
        verify(processor).process(aryEq(body));
    }

    @Test void rejectsUnsignedMalformedAndTamperedSignaturesBeforeProcessing() throws Exception {
        byte[] body = "{\"object\":\"page\"}".getBytes(StandardCharsets.UTF_8);
        mvc.perform(post("/webhook").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/messenger/webhook").content(body).header("X-Hub-Signature-256", "sha256=xyz"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/webhook").content(body).header("X-Hub-Signature-256",
                sign("changed".getBytes(StandardCharsets.UTF_8), "test-app-secret"))).andExpect(status().isForbidden());
        verifyNoInteractions(processor);
    }

    @Test void malformedSignedJsonIsBadRequest() throws Exception {
        byte[] body = "{".getBytes(StandardCharsets.UTF_8);
        mvc.perform(post("/webhook").content(body).header("X-Hub-Signature-256", sign(body, "test-app-secret")))
                .andExpect(status().isBadRequest());
    }

    @Test void persistenceFailureReturnsRetryableStatus() throws Exception {
        byte[] body = "{\"object\":\"page\"}".getBytes(StandardCharsets.UTF_8);
        doThrow(new IllegalStateException("database down")).when(processor).process(any(byte[].class));
        mvc.perform(post("/webhook").content(body).header("X-Hub-Signature-256", sign(body, "test-app-secret")))
                .andExpect(status().isServiceUnavailable()).andExpect(content().string(""));
    }

    @Test void keepsWhatsAppOnLegacySharedUrl() throws Exception {
        mvc.perform(get("/webhook").param("hub.mode", "subscribe").param("hub.verify_token", "wa-verify")
                .param("hub.challenge", "55")).andExpect(status().isOk()).andExpect(content().string("55"));
        byte[] body = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        mvc.perform(post("/webhook").content(body).header("X-Hub-Signature-256", sign(body, "wa-secret")))
                .andExpect(status().isOk());
        verify(whatsApp).process(new String(body, StandardCharsets.UTF_8));
        verifyNoInteractions(processor);
    }

    @Test void cannotUseOtherChannelSecret() throws Exception {
        byte[] body = "{\"object\":\"page\"}".getBytes(StandardCharsets.UTF_8);
        mvc.perform(post("/webhook").content(body).header("X-Hub-Signature-256", sign(body, "wa-secret")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(processor);
    }

    @Test void rejectsOversizedPayload() throws Exception {
        properties.setMaxPayloadBytes(4);
        mvc.perform(post("/webhook").content("too long")).andExpect(status().isPayloadTooLarge());
    }

    private static byte[] aryEq(byte[] body) {
        return org.mockito.AdditionalMatchers.aryEq(body);
    }
}