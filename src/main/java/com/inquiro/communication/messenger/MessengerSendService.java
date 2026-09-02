package com.inquiro.communication.messenger;

import com.inquiro.config.MessengerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessengerSendService {

    private final MessengerProperties properties;

    private final RestClient restClient =
            RestClient.create();

    public void sendText(
            String recipientId,
            String text) {

        String url =
                "https://graph.facebook.com/v26.0/me/messages"
                        + "?access_token="
                        + properties.getPageAccessToken();

        Map<String, Object> body =
                Map.of(
                        "recipient",
                        Map.of("id", recipientId),
                        "message",
                        Map.of("text", text)
                );

        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}