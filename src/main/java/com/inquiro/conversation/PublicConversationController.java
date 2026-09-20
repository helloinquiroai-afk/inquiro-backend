package com.inquiro.conversation;

import com.inquiro.business.BusinessChannelType;
import com.inquiro.inquiry.InquiryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/conversations")
@RequiredArgsConstructor
public class PublicConversationController {

    private final ConversationService conversationService;

    @PostMapping("/message")
    public InquiryResponse message(@Valid @RequestBody ConversationMessageRequest request) {
        if (request.channelId() == null || request.channelId().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "channelId is required");
        }

        return conversationService.process(
                request.sessionId(),
                BusinessChannelType.WEBSITE,
                request.channelId().trim(),
                request.message());
    }

    @DeleteMapping("/{sessionId}")
    public void clear(
            @PathVariable String sessionId,
            @RequestParam String channelId) {
        conversationService.clearPublicSession(sessionId, channelId);
    }
}
