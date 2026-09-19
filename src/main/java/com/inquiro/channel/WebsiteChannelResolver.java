package com.inquiro.channel;

import com.inquiro.business.BusinessChannel;
import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessChannelType;
import org.springframework.stereotype.Service;

@Service
public class WebsiteChannelResolver {

    private final BusinessChannelRepository channelRepository;

    public WebsiteChannelResolver(BusinessChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    public BusinessChannel resolve(String externalChannelId) {
        if (externalChannelId == null || externalChannelId.isBlank()) {
            throw new IllegalArgumentException("Website channel ID is required");
        }

        BusinessChannel channel = channelRepository.findByTypeAndExternalId(
                BusinessChannelType.WEBSITE,
                externalChannelId.trim());

        if (channel == null) {
            throw new IllegalStateException(
                    "No website channel configured for external ID: " + externalChannelId);
        }
        if (!channel.enabled()) {
            throw new IllegalStateException(
                    "Website channel is disabled: " + externalChannelId);
        }

        return channel;
    }
}
