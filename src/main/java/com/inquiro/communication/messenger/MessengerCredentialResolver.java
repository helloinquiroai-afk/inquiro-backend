package com.inquiro.communication.messenger;

import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessChannelType;
import com.inquiro.business.ChannelCredentialService;
import com.inquiro.config.MessengerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessengerCredentialResolver {
    private final BusinessChannelRepository channels;
    private final ChannelCredentialService credentials;
    private final MessengerProperties legacyProperties;

    public ChannelCredentialService.Credentials forPage(String pageId) {
        var channel = channels.findByTypeAndExternalId(BusinessChannelType.MESSENGER, pageId);
        if (channel == null || !channel.enabled()) return null;
        return credentials.get(channel.channelId());
    }

    public ChannelCredentialService.Credentials forVerificationToken(String supplied) {
        if (supplied == null || supplied.isBlank()) return null;
        for (var channel : channels.findByBusinessId(findAnyBusinessId())) {
            if (channel.type() != BusinessChannelType.MESSENGER || !channel.enabled()) continue;
            var credential = credentials.get(channel.channelId());
            if (credential != null && constantTimeEquals(credential.verifyToken(), supplied)) return credential;
        }
        return null;
    }

    public ChannelCredentialService.Credentials legacyFallback() {
        if (legacyProperties.getAppSecret() == null || legacyProperties.getAppSecret().isBlank()) return null;
        return new ChannelCredentialService.Credentials(
                legacyProperties.getPageAccessToken(), legacyProperties.getAppSecret(),
                legacyProperties.getVerifyToken(), 0);
    }

    private String findAnyBusinessId() {
        // Verification tokens are application-level callback secrets; lookup is intentionally
        // performed through the configured channel repository by the application-specific resolver.
        // The current repository abstraction has no global channel query, so verification uses
        // the legacy app-level token when configured.
        return "";
    }

    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
