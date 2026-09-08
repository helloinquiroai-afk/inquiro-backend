package com.inquiro.business;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaBusinessChannelRepository
        implements BusinessChannelRepository {

    private final BusinessChannelJpaRepository jpaRepository;


    @Override
    public BusinessChannel findByTypeAndExternalId(
            BusinessChannelType type,
            String externalId) {

        if (type == null
                || externalId == null
                || externalId.isBlank()) {

            return null;
        }

        return jpaRepository
                .findByTypeAndExternalId(
                        type.name(),
                        externalId
                )
                .map(this::toDomain)
                .orElse(null);
    }


    @Override
    public List<BusinessChannel> findByBusinessId(
            String businessId) {

        if (businessId == null
                || businessId.isBlank()) {

            return List.of();
        }

        return jpaRepository
                .findByBusinessId(businessId)
                .stream()
                .map(this::toDomain)
                .toList();
    }


    @Override
    public void save(
            BusinessChannel channel) {

        if (channel == null) {

            throw new IllegalArgumentException(
                    "Business channel cannot be null"
            );
        }

        BusinessChannelEntity entity =
                new BusinessChannelEntity(
                        channel.channelId(),
                        channel.businessId(),
                        channel.type().name(),
                        channel.externalId(),
                        channel.enabled()
                );

        jpaRepository.save(entity);
    }


    private BusinessChannel toDomain(
            BusinessChannelEntity entity) {

        return new BusinessChannel(
                entity.getChannelId(),
                entity.getBusinessId(),
                BusinessChannelType.valueOf(
                        entity.getType()
                ),
                entity.getExternalId(),
                entity.isEnabled()
        );
    }
}