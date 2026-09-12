package com.inquiro.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, String> {
    Optional<UserAccountEntity> findByEmail(String email);
}
