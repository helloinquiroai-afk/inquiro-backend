package com.inquiro.conversation;

import com.inquiro.business.BusinessChannelType;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.business.BusinessChannel;
import com.inquiro.business.BusinessChannelRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/public/conversations")
@RequiredArgsConstructor
public class PublicConversationController {

    private final ConversationService conversationService;
    private final BusinessChannelRepository businessChannelRepository;

    @Value("${inquiro.widget.host-origins:http://localhost:5173}")
    private String widgetHostOrigins;

    @PostMapping("/message")
    public InquiryResponse message(@Valid @RequestBody ConversationMessageRequest request) {
        if (request.channelId() == null || request.channelId().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "channelId is required");
        }

        String channelId = request.channelId().trim();
        BusinessChannel channel = businessChannelRepository.findByTypeAndExternalId(BusinessChannelType.WEBSITE, channelId);
        if (channel == null || !channel.enabled()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Website channel is not available");
        if (!channel.allowedOrigins().isEmpty()) {
            String siteOrigin = normalizeOrigin(request.siteOrigin());
            if (siteOrigin.isBlank() || (!channel.allowedOrigins().contains(siteOrigin) && !isWidgetHostOrigin(siteOrigin))) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Website origin is not authorized");
        }
        return conversationService.process(request.sessionId(), BusinessChannelType.WEBSITE, channelId, request.message());
    }

    private boolean isWidgetHostOrigin(String origin) {
        return java.util.Arrays.stream(widgetHostOrigins.split(","))
                .map(String::trim)
                .map(PublicConversationController::normalizeOrigin)
                .anyMatch(origin::equals);
    }

    private static String normalizeOrigin(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            java.net.URI uri = java.net.URI.create(value.trim());
            if (uri.getScheme() == null || uri.getHost() == null) return "";
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) return "";
            String path = uri.getRawPath();
            if ((path != null && !path.isBlank() && !"/".equals(path)) || uri.getRawQuery() != null || uri.getRawFragment() != null) return "";
            return value.trim().replaceAll("/$", "");
        } catch (IllegalArgumentException ex) { return ""; }
    }

    @DeleteMapping("/{sessionId}")
    public void clear(
            @PathVariable String sessionId,
            @RequestParam String channelId) {
        conversationService.clearPublicSession(sessionId, channelId);
    }
}
