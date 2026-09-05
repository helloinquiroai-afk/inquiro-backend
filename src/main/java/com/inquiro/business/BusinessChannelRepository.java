package com.inquiro.business;

public interface BusinessChannelRepository {

    BusinessChannel findByTypeAndExternalId(
            BusinessChannelType type,
            String externalId
    );

    void save(
            BusinessChannel channel
    );
}
