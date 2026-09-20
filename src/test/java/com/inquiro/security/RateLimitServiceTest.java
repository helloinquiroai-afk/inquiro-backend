package com.inquiro.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {
    @Test
    void rejectsAfterConfiguredWindowLimit() {
        RateLimitService service = new RateLimitService();
        assertTrue(service.tryAcquire("ip:test", 2).allowed());
        assertTrue(service.tryAcquire("ip:test", 2).allowed());
        assertFalse(service.tryAcquire("ip:test", 2).allowed());
        assertTrue(service.tryAcquire("ip:other", 2).allowed());
    }

    @Test
    void separateKeysDoNotShareQuota() {
        RateLimitService service = new RateLimitService();
        assertTrue(service.tryAcquire("user:a", 1).allowed());
        assertFalse(service.tryAcquire("user:a", 1).allowed());
        assertTrue(service.tryAcquire("user:b", 1).allowed());
    }
}
