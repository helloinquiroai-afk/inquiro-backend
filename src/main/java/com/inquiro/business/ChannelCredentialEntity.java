package com.inquiro.business;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "channel_credential")
public class ChannelCredentialEntity {
    @Id
    @Column(name = "channel_id", nullable = false, updatable = false)
    private String channelId;

    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_app_secret", nullable = false, columnDefinition = "TEXT")
    private String encryptedAppSecret;

    @Column(name = "encrypted_verify_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedVerifyToken;

    @Column(name = "key_version", nullable = false)
    private int keyVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChannelCredentialEntity() {}

    public ChannelCredentialEntity(String channelId, String encryptedAccessToken,
            String encryptedAppSecret, String encryptedVerifyToken, int keyVersion, Instant updatedAt) {
        this.channelId = channelId;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedAppSecret = encryptedAppSecret;
        this.encryptedVerifyToken = encryptedVerifyToken;
        this.keyVersion = keyVersion;
        this.updatedAt = updatedAt;
    }

    public String getChannelId() { return channelId; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public String getEncryptedAppSecret() { return encryptedAppSecret; }
    public String getEncryptedVerifyToken() { return encryptedVerifyToken; }
    public int getKeyVersion() { return keyVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
}
