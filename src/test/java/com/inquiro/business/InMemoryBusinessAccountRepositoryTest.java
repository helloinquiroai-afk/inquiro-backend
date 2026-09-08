package com.inquiro.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBusinessAccountRepositoryTest {

    @Test
    void shouldAllowBusinessAccountWithoutFacebookOrAnyChannel() {

        InMemoryBusinessAccountRepository repository =
                new InMemoryBusinessAccountRepository();

        BusinessProfileProvider profileProvider =
                new BusinessProfileProvider();

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