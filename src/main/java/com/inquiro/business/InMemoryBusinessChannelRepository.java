package com.inquiro.business;

import org.springframework.stereotype.Repository;

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
         * This connects the current Messenger Page ID
         * to the business account.
         *
         * Later this will come from persistent business
         * channel configuration.
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
    public void save(
            BusinessChannel channel) {

        if (channel == null) {
            throw new IllegalArgumentException(
                    "Business channel cannot be null"
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