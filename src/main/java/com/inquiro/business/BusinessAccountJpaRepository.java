package com.inquiro.business;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessAccountJpaRepository
        extends JpaRepository<BusinessAccountEntity, String> {
}
