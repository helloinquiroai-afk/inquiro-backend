package com.inquiro.communication.messenger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.conversation.ConversationService;
import com.inquiro.inquiry.InquiryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessengerMessageProcessor {

    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final MessengerSendService messengerSendService;

    public void process(String payload) {

        try {

            JsonNode root =
                    objectMapper.readTree(payload);

            JsonNode messaging =
                    root.path("entry")
                            .get(0)
                            .path("messaging")
                            .get(0);

            String senderId =
                    messaging.path("sender")
                            .path("id")
                            .asText();

            String recipientId =
                    messaging.path("recipient")
                            .path("id")
                            .asText();

            String messageText =
                    messaging.path("message")
                            .path("text")
                            .asText();

            System.out.println("==================================");
            System.out.println("Messenger Message");
            System.out.println("==================================");
            System.out.println("Sender       : " + senderId);
            System.out.println("Facebook Page: " + recipientId);
            System.out.println("Message      : " + messageText);

            InquiryResponse response =
                    conversationService.process(
                            senderId,
                            recipientId,
                            messageText
                    );

            System.out.println("==================================");
            System.out.println("Inquiro Response");
            System.out.println("==================================");
            System.out.println("Inquiry        : " + response.inquiry());
            System.out.println("Missing Fields : " + response.missingFields());
            System.out.println("Status         : " + response.status());
            System.out.println("Reply          : " + response.reply());

            messengerSendService.sendText(
                    senderId,
                    response.reply()
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}