package com.inquiro.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessAccountRepositoryTest {

    @Test
    void shouldFindBusinessByBusinessId() {

        BusinessProfileProvider profileProvider =
                new BusinessProfileProvider();

        InMemoryBusinessAccountRepository repository =
                new InMemoryBusinessAccountRepository(
                        profileProvider
                );

        BusinessAccount account =
                repository.findByBusinessId(
                        "biz_001"
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

        assertNotNull(
                account.profile()
        );
    }


    @Test
    void shouldReturnNullForUnknownBusinessId() {

        BusinessProfileProvider profileProvider =
                new BusinessProfileProvider();

        InMemoryBusinessAccountRepository repository =
                new InMemoryBusinessAccountRepository(
                        profileProvider
                );

        BusinessAccount account =
                repository.findByBusinessId(
                        "unknown-business"
                );

        assertNull(account);
    }
}