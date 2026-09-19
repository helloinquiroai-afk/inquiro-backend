package com.inquiro.channel;

import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessChannel;
import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.conversation.ConversationIdentity;
import com.inquiro.conversation.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelMessageRouter {

    private final BusinessChannelRepository channelRepository;
    private final BusinessAccountRepository businessAccountRepository;
    private final ConversationService conversationService;

    public ChannelMessageResult route(ChannelMessage message) {
        BusinessChannel channel = channelRepository.findByTypeAndExternalId(
                message.channelType(), message.externalChannelId());

        if (channel == null) {
            throw new IllegalStateException(
                    "No business channel configured for external ID: " + message.externalChannelId());
        }
        if (!channel.enabled()) {
            throw new IllegalStateException(
                    "Business channel is disabled: " + message.externalChannelId());
        }

        BusinessAccount account = businessAccountRepository.findByBusinessId(channel.businessId());
        if (account == null) {
            throw new IllegalStateException(
                    "No business configured for business ID: " + channel.businessId());
        }

        String sessionId = new ConversationIdentity(
                account.businessId(),
                message.channelType(),
                message.externalChannelId(),
                message.customerId()).sessionId();

        return new ChannelMessageResult(
                account.businessId(),
                sessionId,
                conversationService.process(
                        message.customerId(),
                        message.channelType(),
                        message.externalChannelId(),
                        message.text()));
    }
}
