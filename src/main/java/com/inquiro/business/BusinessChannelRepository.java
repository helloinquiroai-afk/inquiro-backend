package com.inquiro.business;

import java.util.List;

public interface BusinessChannelRepository {

    BusinessChannel findByTypeAndExternalId(
            BusinessChannelType type,
            String externalId
    );

    default BusinessChannel findByBusinessIdAndTypeAndExternalId(
            String businessId,
            BusinessChannelType type,
            String externalId) {

        List<BusinessChannel> channels = findByBusinessId(businessId);

        if (channels == null) {
            return null;
        }

        return channels.stream()
                .filter(channel -> channel != null)
                .filter(channel -> businessId.equals(channel.businessId()))
                .filter(channel -> type == channel.type())
                .filter(channel -> externalId.equals(channel.externalId()))
                .findFirst()
                .orElse(null);
    }

    List<BusinessChannel> findByBusinessId(
            String businessId
    );

    List<BusinessChannel> findByType(BusinessChannelType type);

    void save(
            BusinessChannel channel
    );
}
