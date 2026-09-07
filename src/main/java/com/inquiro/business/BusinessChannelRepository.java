package com.inquiro.business;

import java.util.List;

public interface BusinessChannelRepository {

    BusinessChannel findByTypeAndExternalId(
            BusinessChannelType type,
            String externalId
    );

    List<BusinessChannel> findByBusinessId(
            String businessId
    );

    void save(
            BusinessChannel channel
    );
}
