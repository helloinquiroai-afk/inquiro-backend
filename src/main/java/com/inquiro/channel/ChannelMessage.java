package com.inquiro.channel;

import com.inquiro.business.BusinessChannelType;

public record ChannelMessage(
        BusinessChannelType channelType,
        String externalChannelId,
        String customerId,
        String text
) {
    public ChannelMessage {
        if (channelType == null) throw new IllegalArgumentException("Channel type is required");
        if (externalChannelId == null || externalChannelId.isBlank()) {
            throw new IllegalArgumentException("External channel ID is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Message text is required");
    }
}
