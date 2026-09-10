package com.inquiro.communication.whatsapp;

import com.inquiro.communication.messenger.MetaSignatureValidator;
import com.inquiro.config.WhatsAppProperties;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/whatsapp/webhook")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {
    private final WhatsAppProperties properties;
    private final WhatsAppMessageProcessor processor;
    private final MetaSignatureValidator signatures;

    @GetMapping
    public String verify(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {
        if (!"subscribe".equals(mode) || challenge == null
                || !signatures.tokenMatches(properties.getVerifyToken(), token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return challenge;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody byte[] payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        if (payload.length > 1048576) return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        if (!signatures.isValid(payload, signature, properties.getAppSecret())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            processor.process(new String(payload, StandardCharsets.UTF_8));
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            log.warn("event=whatsapp_processing_failed error_type={}", exception.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}