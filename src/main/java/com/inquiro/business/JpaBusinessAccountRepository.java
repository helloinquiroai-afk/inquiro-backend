package com.inquiro.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaBusinessAccountRepository
        implements BusinessAccountRepository {

    private final BusinessAccountJpaRepository jpaRepository;

    private final ObjectMapper objectMapper;

    @Override
    public BusinessAccount findByBusinessIdForUpdate(String businessId) {
        return jpaRepository.findForUpdate(businessId).map(this::toDomain).orElse(null);
    }


    @Override
    public BusinessAccount findByBusinessId(
            String businessId) {

        if (businessId == null
                || businessId.isBlank()) {

            return null;
        }

        return jpaRepository
                .findById(businessId)
                .map(this::toDomain)
                .orElse(null);
    }


    @Override
    public void save(
            BusinessAccount account) {

        if (account == null) {

            throw new IllegalArgumentException(
                    "Business account cannot be null"
            );
        }

        String profileJson =
                serializeProfile(
                        account.profile()
                );

        BusinessAccountEntity entity =
                new BusinessAccountEntity(
                        account.businessId(),
                        account.businessName(),
                        account.profile().businessType(),
                        account.profile().description(),
                        profileJson
                );

        jpaRepository.save(entity);
    }


    private BusinessAccount toDomain(
            BusinessAccountEntity entity) {

        BusinessProfile profile =
                deserializeProfile(
                        entity.getProfileJson()
                );

        return new BusinessAccount(
                entity.getBusinessId(),
                entity.getBusinessName(),
                profile
        );
    }


    private String serializeProfile(
            BusinessProfile profile) {

        try {

            return objectMapper.writeValueAsString(
                    profile
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to serialize business profile",
                    e
            );
        }
    }


    private BusinessProfile deserializeProfile(
            String profileJson) {

        if (profileJson == null
                || profileJson.isBlank()) {

            /*
             * Compatibility fallback for rows created
             * before profile_json was introduced.
             */
            return new BusinessProfile(
                    "",
                    "",
                    "",
                    java.util.List.of(),
                    null
            );
        }

        try {

            return objectMapper.readValue(
                    profileJson,
                    BusinessProfile.class
            );

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to deserialize business profile",
                    e
            );
        }
    }
}
