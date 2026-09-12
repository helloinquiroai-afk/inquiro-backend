package com.inquiro.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final UserAccountJpaRepository users;
    private final AuthSessionJpaRepository sessions;
    private final PasswordEncoder passwordEncoder;

    @Value("${inquiro.auth.session-ttl-hours:24}")
    private long sessionTtlHours;

    @Transactional
    public UserResponse register(String email, String password, String name) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        String normalizedName = normalizeName(name);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email address");
        }

        UserAccountEntity user = new UserAccountEntity(
                "usr_" + UUID.randomUUID(), normalizedEmail, passwordEncoder.encode(password), normalizedName,
                Instant.now(), true);
        users.save(user);
        return toResponse(user);
    }

    @Transactional
    public LoginResponse login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        UserAccountEntity user = users.findByEmail(normalizedEmail)
                .orElseThrow(AuthService::invalidCredentials);
        if (!user.isEnabled() || password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(validSessionTtlHours()));
        String token = newToken();
        sessions.save(new AuthSessionEntity("ses_" + UUID.randomUUID(), user.getUserId(), hashToken(token), now, expiresAt));
        return new LoginResponse(token, "Bearer", expiresAt, toResponse(user));
    }

    @Transactional
    public Optional<AuthenticatedUser> authenticate(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        Optional<AuthSessionEntity> session = sessions.findByTokenHash(hashToken(token));
        if (session.isEmpty()) return Optional.empty();

        AuthSessionEntity activeSession = session.get();
        Instant now = Instant.now();
        if (activeSession.getRevokedAt() != null || !activeSession.getExpiresAt().isAfter(now)) return Optional.empty();

        Optional<UserAccountEntity> user = users.findById(activeSession.getUserId());
        if (user.isEmpty() || !user.get().isEnabled()) return Optional.empty();
        activeSession.markUsed(now);
        return Optional.of(new AuthenticatedUser(user.get().getUserId(), user.get().getEmail()));
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        sessions.findByTokenHash(hashToken(token)).ifPresent(session -> session.revoke(Instant.now()));
    }

    private long validSessionTtlHours() {
        if (sessionTtlHours < 1 || sessionTtlHours > 24 * 31) {
            throw new IllegalStateException("inquiro.auth.session-ttl-hours must be between 1 and 744");
        }
        return sessionTtlHours;
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeEmailForLookup(String email) {
        return normalizeEmail(email);
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required and must be at most 200 characters");
        }
        return name.trim();
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 12 characters and no more than 72 UTF-8 bytes");
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return "inq_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static UserResponse toResponse(UserAccountEntity user) {
        return new UserResponse(user.getUserId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }

    public record UserResponse(String userId, String email, String name, Instant createdAt) { }
    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) { }
}
