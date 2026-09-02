package com.inquiro.conversation;

import com.inquiro.inquiry.InquiryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationStore conversationStore;

    @PostMapping("/message")
    public InquiryResponse message(
            @RequestBody ConversationMessageRequest request) {

        return conversationService.process(
                request.sessionId(),
                "1138575329350155",
                request.message()
        );
    }

    @DeleteMapping("/{sessionId}")
    public void clear(
            @PathVariable String sessionId) {

        conversationStore.remove(sessionId);
    }
}
