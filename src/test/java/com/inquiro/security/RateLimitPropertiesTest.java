package com.inquiro.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RateLimitPropertiesTest {
    @Test
    void hasSafeProductionDefaults() {
        RateLimitProperties p = new RateLimitProperties();
        assertTrue(p.isEnabled());
        assertEquals(60, p.getRequestsPerMinute());
        assertEquals(120, p.getAuthenticatedRequestsPerMinute());
        assertEquals(20, p.getPublicAiRequestsPerMinute());
        assertEquals(10, p.getAuthRequestsPerMinute());
        assertEquals(120, p.getWebhookRequestsPerMinute());
        assertEquals(1_048_576, p.getMaxRequestBodyBytes());
    }
}
