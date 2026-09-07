package com.inquiro.business;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBusinessChannelRepository
        implements BusinessChannelRepository {

    private final Map<String, BusinessChannel> channels =
            new ConcurrentHashMap<>();

    public InMemoryBusinessChannelRepository() {

        /*
         * Temporary development configuration.
         *
         * Existing Messenger connection for biz_001.
         */
        save(
                new BusinessChannel(
                        "channel_messenger_001",
                        "biz_001",
                        BusinessChannelType.MESSENGER,
                        "1138575329350155",
                        true
                )
        );
    }

    @Override
    public BusinessChannel findByTypeAndExternalId(
            BusinessChannelType type,
            String externalId) {

        if (type == null || externalId == null) {
            return null;
        }

        return channels.get(
                buildKey(
                        type,
                        externalId
                )
        );
    }

    @Override
    public List<BusinessChannel> findByBusinessId(
            String businessId) {

        if (businessId == null || businessId.isBlank()) {
            return List.of();
        }

        return new ArrayList<>(
                channels.values()
                        .stream()
                        .filter(channel ->
                                businessId.equals(
                                        channel.businessId()
                                )
                        )
                        .toList()
        );
    }

    @Override
    public void save(
            BusinessChannel channel) {

        if (channel == null) {
            throw new IllegalArgumentException(
                    "Business channel cannot be null"
            );
        }

        if (channel.channelId() == null
                || channel.channelId().isBlank()) {

            throw new IllegalArgumentException(
                    "Channel ID cannot be null or blank"
            );
        }

        if (channel.businessId() == null
                || channel.businessId().isBlank()) {

            throw new IllegalArgumentException(
                    "Business ID cannot be null or blank"
            );
        }

        if (channel.type() == null) {

            throw new IllegalArgumentException(
                    "Channel type cannot be null"
            );
        }

        if (channel.externalId() == null
                || channel.externalId().isBlank()) {

            throw new IllegalArgumentException(
                    "External ID cannot be null or blank"
            );
        }

        channels.put(
                buildKey(
                        channel.type(),
                        channel.externalId()
                ),
                channel
        );
    }

    private String buildKey(
            BusinessChannelType type,
            String externalId) {

        return type.name()
                + ":"
                + externalId;
    }
}