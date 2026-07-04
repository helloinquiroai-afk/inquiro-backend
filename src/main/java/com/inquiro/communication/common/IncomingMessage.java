package com.inquiro.communication.common;

public record IncomingMessage(

        String sessionId,

        String senderId,

        String message

) {
}
