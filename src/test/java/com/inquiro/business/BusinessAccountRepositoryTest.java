package com.inquiro.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessAccountRepositoryTest {

    @Test
    void shouldFindBusinessByFacebookPageId() {

        BusinessProfileProvider profileProvider =
                new BusinessProfileProvider();

        InMemoryBusinessAccountRepository repository =
                new InMemoryBusinessAccountRepository(
                        profileProvider
                );

        BusinessAccount account =
                repository.findByFacebookPageId(
                        "1138575329350155"
                );

        assertNotNull(account);

        assertEquals(
                "biz_001",
                account.businessId()
        );

        assertEquals(
                "ABC Auto Care",
                account.businessName()
        );

        assertEquals(
                "1138575329350155",
                account.facebookPageId()
        );

        assertNotNull(account.profile());
    }

    @Test
    void shouldReturnNullForUnknownFacebookPage() {

        BusinessProfileProvider profileProvider =
                new BusinessProfileProvider();

        InMemoryBusinessAccountRepository repository =
                new InMemoryBusinessAccountRepository(
                        profileProvider
                );

        BusinessAccount account =
                repository.findByFacebookPageId(
                        "unknown-page"
                );

        assertNull(account);
    }
}
