package com.inquiro.conversation;

public record ConversationMessageRequest(

        String sessionId,

        String message

) {
}
