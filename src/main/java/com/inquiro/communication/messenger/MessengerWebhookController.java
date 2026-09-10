package com.inquiro.communication.messenger;

import com.inquiro.config.MessengerProperties;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/messenger/webhook")
@RequiredArgsConstructor
@Slf4j
public class MessengerWebhookController {
    private final MessengerProperties properties;
    private final MessengerMessageProcessor processor;
    private final MetaSignatureValidator signatures;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
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
        if (payload.length > properties.getMaxPayloadBytes()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        if (!signatures.isValid(payload, signature, properties.getAppSecret())) {
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