package com.inquiro.business;

import com.inquiro.security.SecretEncryptionService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelCredentialService {
    private final ChannelCredentialJpaRepository credentials;
    private final SecretEncryptionService encryption;

    @Value("${inquiro.security.credential-key-version:1}")
    private int keyVersion;

    public void save(String channelId, String accessToken, String appSecret, String verifyToken) {
        requireSecret(accessToken, "accessToken");
        requireSecret(appSecret, "appSecret");
        requireSecret(verifyToken, "verifyToken");
        credentials.save(new ChannelCredentialEntity(channelId,
                encryption.encrypt(accessToken), encryption.encrypt(appSecret),
                encryption.encrypt(verifyToken), keyVersion, Instant.now()));
    }

    public Credentials get(String channelId) {
        return credentials.findById(channelId)
                .map(entity -> new Credentials(
                        encryption.decrypt(entity.getEncryptedAccessToken()),
                        encryption.decrypt(entity.getEncryptedAppSecret()),
                        encryption.decrypt(entity.getEncryptedVerifyToken()),
                        entity.getKeyVersion()))
                .orElse(null);
    }

    public boolean hasCredentials(String channelId) {
        return credentials.existsById(channelId);
    }

    public void delete(String channelId) {
        credentials.deleteById(channelId);
    }

    private static void requireSecret(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }

    public record Credentials(String accessToken, String appSecret, String verifyToken, int keyVersion) {}
}
