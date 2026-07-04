package com.inquiro.communication.whatsapp;

import com.inquiro.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppProperties properties;

    @GetMapping
    public String verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {

        if (!properties.getVerifyToken().equals(verifyToken)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid verify token."
            );
        }

        return challenge;
    }

    @PostMapping
    public ResponseEntity<Void> receive() {

        System.out.println("WhatsApp webhook received.");

        return ResponseEntity.ok().build();
    }
}