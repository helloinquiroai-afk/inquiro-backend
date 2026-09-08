package com.inquiro.business;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BusinessJpaRepositoryTest {

    @Autowired
    private BusinessAccountJpaRepository businessAccountJpaRepository;

    @Autowired
    private BusinessChannelJpaRepository businessChannelJpaRepository;

    @Test
    void shouldSaveAndFindBusinessAccount() {

        BusinessAccountEntity entity =
                new BusinessAccountEntity(
                        "biz_test_001",
                        "Test Dental Care",
                        "DENTAL_CLINIC",
                        "Test dental clinic",
                        "{\"businessName\":\"Test Dental Care\"}"
                );

        businessAccountJpaRepository.save(entity);

        BusinessAccountEntity found =
                businessAccountJpaRepository
                        .findById("biz_test_001")
                        .orElse(null);

        assertNotNull(found);

        assertEquals(
                "biz_test_001",
                found.getBusinessId()
        );

        assertEquals(
                "Test Dental Care",
                found.getBusinessName()
        );

        assertEquals(
                "DENTAL_CLINIC",
                found.getBusinessType()
        );
    }

    @Test
    void shouldSaveAndFindBusinessChannel() {

        BusinessChannelEntity entity =
                new BusinessChannelEntity(
                        "channel_test_001",
                        "biz_test_001",
                        "WHATSAPP",
                        "test-whatsapp-001",
                        true
                );

        businessChannelJpaRepository.save(entity);

        BusinessChannelEntity found =
                businessChannelJpaRepository
                        .findById("channel_test_001")
                        .orElse(null);

        assertNotNull(found);

        assertEquals(
                "channel_test_001",
                found.getChannelId()
        );

        assertEquals(
                "biz_test_001",
                found.getBusinessId()
        );

        assertEquals(
                "WHATSAPP",
                found.getType()
        );

        assertEquals(
                "test-whatsapp-001",
                found.getExternalId()
        );

        assertTrue(
                found.isEnabled()
        );
    }
}