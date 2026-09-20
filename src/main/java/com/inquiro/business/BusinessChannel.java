package com.inquiro.business;

import java.util.List;

public record BusinessChannel(
        String channelId,
        String businessId,
        BusinessChannelType type,
        String externalId,
        boolean enabled,
        List<String> allowedOrigins
) {
    public BusinessChannel {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(origin -> origin == null ? "" : origin.trim())
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();
    }

    public BusinessChannel(String channelId, String businessId, BusinessChannelType type,
                           String externalId, boolean enabled) {
        this(channelId, businessId, type, externalId, enabled, List.of());
    }

}
