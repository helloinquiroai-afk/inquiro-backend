package com.inquiro.business;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBusinessAccountRepository
        implements BusinessAccountRepository {

    private final Map<String, BusinessAccount> accountsByFacebookPageId =
            new ConcurrentHashMap<>();

    public InMemoryBusinessAccountRepository(
            BusinessProfileProvider profileProvider) {

        BusinessProfile profile =
                profileProvider.get();

        save(
                new BusinessAccount(
                        "biz_001",
                        profile.businessName(),
                        "1138575329350155",
                        profile
                )
        );
    }

    @Override
    public BusinessAccount findByFacebookPageId(
            String facebookPageId) {

        return accountsByFacebookPageId.get(
                facebookPageId
        );
    }

    @Override
    public void save(BusinessAccount account) {

        accountsByFacebookPageId.put(
                account.facebookPageId(),
                account
        );
    }
}
