package com.inquiro.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class SecretEncryptionServiceTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String PREVIOUS = Base64.getEncoder().encodeToString(new byte[] {
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1
    });

    @Test
    void encryptsAndDecryptsWithoutStoringPlaintext() {
        SecretEncryptionService service = new SecretEncryptionService(KEY, "", 2);
        String encrypted = service.encrypt("super-secret-page-token");
        assertNotEquals("super-secret-page-token", encrypted);
        assertTrue(encrypted.startsWith("v2:"));
        assertEquals("super-secret-page-token", service.decrypt(encrypted));
    }

    @Test
    void decryptsWithPreviousKeyDuringRotation() {
        SecretEncryptionService old = new SecretEncryptionService(PREVIOUS, "", 1);
        String encrypted = old.encrypt("old-secret");
        SecretEncryptionService rotated = new SecretEncryptionService(KEY, PREVIOUS, 2);
        assertEquals("old-secret", rotated.decrypt(encrypted));
        String newEncrypted = rotated.encrypt("new-secret");
        assertEquals("new-secret", rotated.decrypt(newEncrypted));
    }

    @Test
    void rejectsInvalidKeyMaterial() {
        assertThrows(IllegalStateException.class, () -> new SecretEncryptionService(
                Base64.getEncoder().encodeToString(new byte[16]), "", 1));
    }
}
