package com.inquiro.communication.messenger;

import com.inquiro.config.MessengerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/messenger/webhook")
@Slf4j
public class MessengerWebhookController {
    @Autowired
    public MessengerWebhookController(MessengerProperties properties, MessengerMessageProcessor processor,
            MetaSignatureValidator signatures, MessengerCredentialResolver credentialResolver, ObjectMapper mapper) {
        this.properties = properties;
        this.processor = processor;
        this.signatures = signatures;
        this.credentialResolver = credentialResolver;
        this.mapper = mapper;
    }

    // Backward-compatible constructor for focused unit tests.
    public MessengerWebhookController(MessengerProperties properties, MessengerMessageProcessor processor,
            MetaSignatureValidator signatures) {
        this.properties = properties;
        this.processor = processor;
        this.signatures = signatures;
        this.credentialResolver = null;
        this.mapper = new ObjectMapper();
    }
    private final MessengerProperties properties;
    private final MessengerMessageProcessor processor;
    private final MetaSignatureValidator signatures;
    private final MessengerCredentialResolver credentialResolver;
    private final ObjectMapper mapper;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {
        boolean valid = credentialResolver == null
                ? signatures.tokenMatches(properties.getVerifyToken(), token)
                : credentialResolver.forVerificationToken(token) != null;
        if (!"subscribe".equals(mode) || challenge == null || !valid) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return challenge;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody byte[] payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        if (payload.length > properties.getMaxPayloadBytes()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        String appSecret = properties.getAppSecret();
        if (credentialResolver != null) {
            try {
                var root = mapper.readTree(payload);
                String pageId = root.path("entry").path(0).path("messaging").path(0)
                        .path("recipient").path("id").asText("");
                var credential = credentialResolver.forPage(pageId);
                if (credential == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                appSecret = credential.appSecret();
            } catch (IOException exception) {
                return ResponseEntity.badRequest().build();
            }
        }
        if (!signatures.isValid(payload, signature, appSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            processor.process(payload);
            return ResponseEntity.ok().build();
        } catch (IOException exception) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException exception) {
            log.error("event=messenger_accept_failed error_type={}", exception.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}