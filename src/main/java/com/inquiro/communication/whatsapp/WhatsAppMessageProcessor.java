package com.inquiro.communication.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.conversation.ConversationService;
import com.inquiro.inquiry.InquiryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WhatsAppMessageProcessor {

    private final ConversationService conversationService;
    private final WhatsAppSender whatsAppSender;
    private final ObjectMapper objectMapper;

    public void process(String payload) throws Exception {

        JsonNode root =
                objectMapper.readTree(payload);

        JsonNode value =
                root.path("entry")
                        .path(0)
                        .path("changes")
                        .path(0)
                        .path("value");

        JsonNode messages =
                value.path("messages");

        if (!messages.isArray() || messages.isEmpty()) {

            System.out.println(
                    "Ignoring non-message webhook"
            );

            return;
        }

        JsonNode message =
                messages.get(0);

        /*
         * =====================================================
         * CUSTOMER
         * =====================================================
         */

        String sender =
                message.path("from")
                        .asText();

        /*
         * =====================================================
         * BUSINESS
         *
         * WhatsApp identifies the business using
         * phone_number_id.
         * =====================================================
         */

        String businessId =
                value.path("metadata")
                        .path("phone_number_id")
                        .asText();

        /*
         * =====================================================
         * MESSAGE
         * =====================================================
         */

        String text =
                message.path("text")
                        .path("body")
                        .asText();

        System.out.println(
                "=================================="
        );

        System.out.println(
                "WhatsApp Message"
        );

        System.out.println(
                "=================================="
        );

        System.out.println(
                "Sender     : " + sender
        );

        System.out.println(
                "BusinessId : " + businessId
        );

        System.out.println(
                "Message    : " + text
        );

        /*
         * =====================================================
         * INQUIRO
         * =====================================================
         */

        InquiryResponse response =
                conversationService.process(
                        sender,
                        businessId,
                        text
                );

        /*
         * =====================================================
         * SEND RESPONSE
         * =====================================================
         */

        whatsAppSender.send(
                sender,
                response.reply()
        );
    }
}