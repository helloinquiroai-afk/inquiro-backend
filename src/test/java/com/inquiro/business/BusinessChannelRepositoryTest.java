package com.inquiro.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessChannelRepositoryTest {

    @Test
    void shouldFindMessengerChannelByExternalId() {

        InMemoryBusinessChannelRepository repository =
                new InMemoryBusinessChannelRepository();

        BusinessChannel channel =
                repository.findByTypeAndExternalId(
                        BusinessChannelType.MESSENGER,
                        "1138575329350155"
                );

        assertNotNull(channel);

        assertEquals(
                "channel_messenger_001",
                channel.channelId()
        );

        assertEquals(
                "biz_001",
                channel.businessId()
        );

        assertEquals(
                BusinessChannelType.MESSENGER,
                channel.type()
        );

        assertEquals(
                "1138575329350155",
                channel.externalId()
        );

        assertTrue(
                channel.enabled()
        );
    }


    @Test
    void shouldReturnNullForUnknownChannel() {

        InMemoryBusinessChannelRepository repository =
                new InMemoryBusinessChannelRepository();

        BusinessChannel channel =
                repository.findByTypeAndExternalId(
                        BusinessChannelType.MESSENGER,
                        "unknown-page"
                );

        assertNull(channel);
    }
}