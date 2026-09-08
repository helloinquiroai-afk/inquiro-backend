package com.inquiro.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessAccountRepositoryTest {

    @Test
    void shouldFindBusinessByBusinessId() {

        InMemoryBusinessAccountRepository repository =
                new InMemoryBusinessAccountRepository();

        BusinessProfile profile =
                new BusinessProfile(
                        "ABC Auto Care",
                        "VEHICLE_SERVICE_CENTER",
                        "Vehicle maintenance and inspection services.",
                        java.util.List.of(),
                        null
                );

        repository.save(
                new BusinessAccount(
                        "biz_001",
                        "ABC Auto Care",
                        profile
                )
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

        InMemoryBusinessAccountRepository repository =
                new InMemoryBusinessAccountRepository();

        BusinessAccount account =
                repository.findByBusinessId(
                        "unknown-business"
                );

        assertNull(account);
    }
}