package com.inquiro.security;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretEncryptionService {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private final byte[] key;
    private final byte[] previousKey;
    private final int keyVersion;
    private final SecureRandom random = new SecureRandom();

    public SecretEncryptionService(
            @Value("${inquiro.security.credential-encryption-key:}") String encodedKey,
            @Value("${inquiro.security.credential-encryption-key-previous:}") String previousEncodedKey,
            @Value("${inquiro.security.credential-key-version:1}") int keyVersion) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("INQUIRO_CREDENTIAL_ENCRYPTION_KEY must be configured");
        }
        try {
            this.key = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("INQUIRO_CREDENTIAL_ENCRYPTION_KEY must be base64", exception);
        }
        if (key.length != 32) throw new IllegalStateException("INQUIRO_CREDENTIAL_ENCRYPTION_KEY must decode to 32 bytes");
        this.previousKey = decodeOptional(previousEncodedKey);
        if (this.previousKey != null && this.previousKey.length != 32) {
            throw new IllegalStateException("INQUIRO_CREDENTIAL_ENCRYPTION_KEY_PREVIOUS must decode to 32 bytes");
        }
        if (keyVersion < 1) throw new IllegalStateException("INQUIRO_CREDENTIAL_KEY_VERSION must be positive");
        this.keyVersion = keyVersion;
    }

    private byte[] decodeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Base64.getDecoder().decode(value); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("Previous credential key must be base64", exception); }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "v" + keyVersion + ":" + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv).put(ciphertext).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt credential", exception);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) return null;
        try {
            int separator = encoded.indexOf(':');
            int version = separator > 0 && encoded.charAt(0) == 'v'
                    ? Integer.parseInt(encoded.substring(1, separator)) : keyVersion;
            String payload = separator > 0 ? encoded.substring(separator + 1) : encoded;
            byte[] packed = Base64.getDecoder().decode(payload);
            if (packed.length <= IV_LENGTH) throw new IllegalArgumentException("Invalid encrypted credential");
            byte[] decryptionKey = version == keyVersion ? key : previousKey;
            if (decryptionKey == null) throw new IllegalStateException("Credential key version " + version + " is not configured");
            byte[] iv = java.util.Arrays.copyOfRange(packed, 0, IV_LENGTH);
            byte[] ciphertext = java.util.Arrays.copyOfRange(packed, IV_LENGTH, packed.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptionKey, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt credential", exception);
        }
    }
}
