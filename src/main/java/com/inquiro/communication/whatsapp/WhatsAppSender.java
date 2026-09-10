package com.inquiro.communication.whatsapp;

import com.inquiro.communication.whatsapp.model.*;
import com.inquiro.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class WhatsAppSender {

    private final WhatsAppProperties properties;

    public void send(
            String recipient,
            String message) {


/*WhatsAppTemplateRequest request =
                new WhatsAppTemplateRequest(
                        "whatsapp",
                        recipient,
                        "template",
                        new Template(
                                "hello_world",
                                new Language("en_US")
                        )
                );*/

        WhatsAppMessageRequest request =
                new WhatsAppMessageRequest(
                        "whatsapp",
                        recipient,
                        "text",
                        new Text(message)
                );

        RestClient client = RestClient.builder()
                .baseUrl("https://graph.facebook.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getAccessToken()
                )
                .build();

        try {

            String response = client.post()
                    .uri("/v25.0/" +
                            properties.getPhoneNumberId() +
                            "/messages")
                    .body(request)
                    .retrieve()
                    .body(String.class);

} catch (Exception e) {

            log.warn("event=whatsapp_send_failed error_type={}", e.getClass().getSimpleName());
        }

    }

}
