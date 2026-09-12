package com.inquiro.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionEntity, String> {
    Optional<AuthSessionEntity> findByTokenHash(String tokenHash);
}
