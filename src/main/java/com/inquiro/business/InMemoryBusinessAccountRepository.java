package com.inquiro.business;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBusinessAccountRepository
        implements BusinessAccountRepository {

    private final Map<String, BusinessAccount> accounts =
            new ConcurrentHashMap<>();

    public InMemoryBusinessAccountRepository(
            BusinessProfileProvider profileProvider) {

        BusinessProfile profile =
                profileProvider.get();

        save(
                new BusinessAccount(
                        "biz_001",
                        profile.businessName(),
                        profile
                )
        );
    }

    @Override
    public BusinessAccount findByBusinessId(
            String businessId) {

        if (businessId == null || businessId.isBlank()) {
            return null;
        }

        return accounts.get(
                businessId
        );
    }

    @Override
    public void save(
            BusinessAccount account) {

        if (account == null) {
            throw new IllegalArgumentException(
                    "Business account cannot be null"
            );
        }

        if (account.businessId() == null
                || account.businessId().isBlank()) {

            throw new IllegalArgumentException(
                    "Business ID cannot be null or blank"
            );
        }

        accounts.put(
                account.businessId(),
                account
        );
    }
}