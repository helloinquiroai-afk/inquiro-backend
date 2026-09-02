package com.inquiro.communication.messenger;

import com.inquiro.config.MessengerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/messenger/webhook")
@RequiredArgsConstructor
public class MessengerWebhookController {

    private final MessengerProperties properties;
    private final MessengerMessageProcessor processor;

    @GetMapping
    public String verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {

        System.out.println("Messenger Verification");
        System.out.println("Mode      : " + mode);
        System.out.println("Token     : " + verifyToken);
        System.out.println("Challenge : " + challenge);

        if (!"subscribe".equals(mode)
                || !properties.getVerifyToken().equals(verifyToken)) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return challenge;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String payload) {

        try {
            System.out.println(payload);
            processor.process(payload);

        } catch (Exception e) {

            e.printStackTrace();
        }

        return ResponseEntity.ok().build();
    }
}
