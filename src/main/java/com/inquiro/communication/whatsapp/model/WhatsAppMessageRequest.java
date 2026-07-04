package com.inquiro.communication.whatsapp.model;

public record WhatsAppMessageRequest(

        String messaging_product,

        String to,

        String type,

        Text text

) {
}
