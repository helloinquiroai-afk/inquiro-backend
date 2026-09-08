package com.inquiro.business;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessChannelJpaRepository
        extends JpaRepository<BusinessChannelEntity, String> {

    Optional<BusinessChannelEntity> findByTypeAndExternalId(
            String type,
            String externalId
    );

    List<BusinessChannelEntity> findByBusinessId(
            String businessId
    );
}
