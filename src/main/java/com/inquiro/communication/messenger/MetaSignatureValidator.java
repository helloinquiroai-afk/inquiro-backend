package com.inquiro.communication.messenger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class MetaSignatureValidator {
    public boolean isValid(byte[] body, String signature, String secret) {
        if (secret == null || secret.isBlank() || signature == null
                || !signature.matches("sha256=[0-9a-fA-F]{64}")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return MessageDigest.isEqual(mac.doFinal(body), HexFormat.of().parseHex(signature.substring(7)));
        } catch (java.security.GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean tokenMatches(String expected, String supplied) {
        return expected != null && !expected.isBlank() && supplied != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                        supplied.getBytes(StandardCharsets.UTF_8));
    }
}