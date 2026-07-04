package com.inquiro.communication.whatsapp.model;

public record WhatsAppTemplateRequest(

        String messaging_product,

        String to,

        String type,

        Template template

) {
}
