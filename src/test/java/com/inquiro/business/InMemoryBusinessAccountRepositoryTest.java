package com.inquiro.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBusinessAccountRepositoryTest {

    @Test
    void shouldAllowBusinessAccountWithoutFacebookOrAnyChannel() {

        BusinessProfileProvider profileProvider =
                new BusinessProfileProvider();

        InMemoryBusinessAccountRepository repository =
                new InMemoryBusinessAccountRepository(
                        profileProvider
                );

        BusinessProfile profile =
                profileProvider.get();

        BusinessAccount account =
                new BusinessAccount(
                        "biz_002",
                        "XYZ Dental Care",
                        profile
                );

        repository.save(account);

        BusinessAccount found =
                repository.findByBusinessId(
                        "biz_002"
                );

        assertNotNull(found);

        assertEquals(
                "biz_002",
                found.businessId()
        );

        assertEquals(
                "XYZ Dental Care",
                found.businessName()
        );
    }
}