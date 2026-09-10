package com.inquiro.communication.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.communication.messenger.MessengerWebhookController;
import com.inquiro.communication.messenger.MetaSignatureValidator;
import com.inquiro.communication.whatsapp.WhatsAppWebhookController;
import com.inquiro.config.MessengerProperties;
import com.inquiro.config.WhatsAppProperties;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Shared callback preserves the existing WhatsApp URL while adding Messenger at /webhook. */
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class MetaWebhookController {
    private final MessengerWebhookController messenger;
    private final WhatsAppWebhookController whatsApp;
    private final MessengerProperties messengerProperties;
    private final WhatsAppProperties whatsAppProperties;
    private final MetaSignatureValidator signatures;
    private final ObjectMapper mapper;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {
        if (signatures.tokenMatches(messengerProperties.getVerifyToken(), token)) {
            return messenger.verify(mode, token, challenge);
        }
        if (signatures.tokenMatches(whatsAppProperties.getVerifyToken(), token)) {
            return whatsApp.verify(mode, token, challenge);
        }
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody byte[] payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        if (payload.length > messengerProperties.getMaxPayloadBytes()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        // Authenticate before parsing even for the shared legacy route.
        boolean messengerSignature = signatures.isValid(payload, signature, messengerProperties.getAppSecret());
        boolean whatsAppSignature = signatures.isValid(payload, signature, whatsAppProperties.getAppSecret());
        if (!messengerSignature && !whatsAppSignature) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            var root = mapper.readTree(payload);
            if (root == null || !root.isObject()) return ResponseEntity.badRequest().build();
            if ("whatsapp_business_account".equals(root.path("object").asText())) {
                return whatsApp.receive(payload, signature);
            }
            return messenger.receive(payload, signature);
        } catch (IOException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}