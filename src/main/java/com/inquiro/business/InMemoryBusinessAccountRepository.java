package com.inquiro.business;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBusinessAccountRepository
        implements BusinessAccountRepository {

    private final Map<String, BusinessAccount> accounts =
            new ConcurrentHashMap<>();

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