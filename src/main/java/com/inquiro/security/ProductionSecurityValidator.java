package com.inquiro.security;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Fails fast when production is started with unsafe placeholder or missing secrets.
 * Optional channel credentials are deliberately not required here because a deployment
 * may start before Messenger/WhatsApp is connected.
 */
@Component
@Profile("prod")
public class ProductionSecurityValidator {

    private final String openAiKey;
    private final String managementKey;
    private final String credentialEncryptionKey;
    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String allowedOrigins;
    private final boolean seedDefaultBusiness;
    private final boolean h2ConsoleEnabled;

    public ProductionSecurityValidator(
            @Value("${openai.api-key:}") String openAiKey,
            @Value("${inquiro.management-api-key:}") String managementKey,
            @Value("${inquiro.security.credential-encryption-key:}") String credentialEncryptionKey,
            @Value("${spring.datasource.url:}") String databaseUrl,
            @Value("${spring.datasource.username:}") String databaseUsername,
            @Value("${spring.datasource.password:}") String databasePassword,
            @Value("${inquiro.security.allowed-origins:}") String allowedOrigins,
            @Value("${inquiro.seed-default-business:false}") boolean seedDefaultBusiness,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) {
        this.openAiKey = openAiKey;
        this.managementKey = managementKey;
        this.credentialEncryptionKey = credentialEncryptionKey;
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.allowedOrigins = allowedOrigins;
        this.seedDefaultBusiness = seedDefaultBusiness;
        this.h2ConsoleEnabled = h2ConsoleEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        requireSecret("OPENAI_API_KEY", openAiKey);
        requireSecret("INQUIRO_MANAGEMENT_API_KEY", managementKey);
        requireSecret("INQUIRO_CREDENTIAL_ENCRYPTION_KEY", credentialEncryptionKey);
        if (!isPostgresJdbcUrl(databaseUrl)) {
            throw new IllegalStateException("Production requires DATABASE_URL to use a PostgreSQL JDBC URL");
        }
        requireNonBlank("DATABASE_USERNAME", databaseUsername);
        requireNonBlank("DATABASE_PASSWORD", databasePassword);
        if (seedDefaultBusiness) {
            throw new IllegalStateException("SEED_DEFAULT_BUSINESS must be false in production");
        }
        if (h2ConsoleEnabled) {
            throw new IllegalStateException("H2 console must be disabled in production");
        }
        if (allowedOrigins.isBlank()) {
            throw new IllegalStateException("INQUIRO_SECURITY_ALLOWED_ORIGINS must contain the trusted frontend origin(s) in production");
        }
        validateBase64Aes256(credentialEncryptionKey);
    }

    private static void requireSecret(String name, String value) {
        requireNonBlank(name, value);
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("CHANGE_ME") || normalized.contains("YOUR_") || normalized.contains("EXAMPLE")) {
            throw new IllegalStateException(name + " contains a placeholder value");
        }
    }

    private static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured in production");
        }
    }

    private static boolean isPostgresJdbcUrl(String value) {
        return value != null && value.startsWith("jdbc:postgresql://");
    }

    private static void validateBase64Aes256(String value) {
        try {
            if (Base64.getDecoder().decode(value).length != 32) {
                throw new IllegalStateException("INQUIRO_CREDENTIAL_ENCRYPTION_KEY must decode to 32 bytes");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("INQUIRO_CREDENTIAL_ENCRYPTION_KEY must be valid base64", exception);
        }
    }
}