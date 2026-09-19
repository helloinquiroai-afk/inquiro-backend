package com.inquiro.conversation;

public record ConversationMessageRequest(

        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String sessionId,

        @jakarta.validation.constraints.Size(max = 128) String channelId,

        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 10000) String message

) {
    public ConversationMessageRequest(String sessionId, String message) {
        this(sessionId, null, message);
    }
}
