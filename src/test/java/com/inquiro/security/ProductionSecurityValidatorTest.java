package com.inquiro.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionSecurityValidatorTest {

    private static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private static ProductionSecurityValidator validator(
            String openAi,
            String management,
            String encryption,
            String dbUrl,
            String dbUser,
            String dbPassword,
            String origins,
            boolean seed,
            boolean h2) {
        return new ProductionSecurityValidator(
                openAi, management, encryption, dbUrl, dbUser, dbPassword, origins, seed, h2);
    }

    @Test
    void acceptsSafeProductionConfiguration() {
        assertDoesNotThrow(() -> validator(
                "sk-real-key",
                "long-random-management-key",
                KEY,
                "jdbc:postgresql://db:5432/inquiro",
                "inquiro",
                "strong-db-password",
                "https://app.example.com",
                false,
                false).validate());
    }

    @Test
    void rejectsPlaceholderSecret() {
        assertThrows(IllegalStateException.class, () -> validator(
                "CHANGE_ME", "long-random-management-key", KEY,
                "jdbc:postgresql://db:5432/inquiro", "inquiro", "password",
                "https://app.example.com", false, false).validate());
    }

    @Test
    void rejectsNonPostgresDatabase() {
        assertThrows(IllegalStateException.class, () -> validator(
                "sk-real-key", "long-random-management-key", KEY,
                "jdbc:h2:file:./data/inquiro", "sa", "password",
                "https://app.example.com", false, false).validate());
    }

    @Test
    void rejectsUnsafeProductionFlags() {
        assertThrows(IllegalStateException.class, () -> validator(
                "sk-real-key", "long-random-management-key", KEY,
                "jdbc:postgresql://db:5432/inquiro", "inquiro", "password",
                "https://app.example.com", true, false).validate());
        assertThrows(IllegalStateException.class, () -> validator(
                "sk-real-key", "long-random-management-key", KEY,
                "jdbc:postgresql://db:5432/inquiro", "inquiro", "password",
                "https://app.example.com", false, true).validate());
    }

    @Test
    void rejectsMissingTrustedOrigin() {
        assertThrows(IllegalStateException.class, () -> validator(
                "sk-real-key", "long-random-management-key", KEY,
                "jdbc:postgresql://db:5432/inquiro", "inquiro", "password",
                "", false, false).validate());
    }

    @Test
    void rejectsInvalidEncryptionKey() {
        assertThrows(IllegalStateException.class, () -> validator(
                "sk-real-key", "long-random-management-key", "not-base64",
                "jdbc:postgresql://db:5432/inquiro", "inquiro", "password",
                "https://app.example.com", false, false).validate());
    }
}
