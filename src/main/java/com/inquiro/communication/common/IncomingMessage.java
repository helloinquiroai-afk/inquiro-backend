package com.inquiro.communication.common;

public record IncomingMessage(

        String sessionId,

        String businessId,

        String senderId,

        String message

) {
}