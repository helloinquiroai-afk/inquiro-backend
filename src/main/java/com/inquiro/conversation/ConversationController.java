package com.inquiro.conversation;

import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.business.BusinessChannelType;
import com.inquiro.business.BusinessChannelRepository;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final BusinessChannelRepository businessChannelRepository;

    @Value("${inquiro.website-channel-id:website-default}")
    private String websiteChannelId;

    @PostMapping("/message")
    public InquiryResponse message(
            @Valid @RequestBody ConversationMessageRequest request) {

        return conversationService.process(
                request.sessionId(),
                BusinessChannelType.WEBSITE,
                websiteChannelId,
                request.message()
        );
    }

    @DeleteMapping("/{sessionId}")
    public void clear(
            @PathVariable String sessionId) {

        var channel = businessChannelRepository.findByTypeAndExternalId(BusinessChannelType.WEBSITE, websiteChannelId);
        if (channel != null && channel.enabled()) {
            conversationRepository.remove(new ConversationIdentity(channel.businessId(), BusinessChannelType.WEBSITE,
                    websiteChannelId, sessionId).sessionId());
        }
    }
}
