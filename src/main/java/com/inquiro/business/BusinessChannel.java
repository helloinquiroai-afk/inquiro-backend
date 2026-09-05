package com.inquiro.business;

public record BusinessChannel(

        String channelId,

        String businessId,

        BusinessChannelType type,

        String externalId,

        boolean enabled

) {
}