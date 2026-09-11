package com.inquiro.business;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessAccountJpaRepository
        extends JpaRepository<BusinessAccountEntity, String> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from BusinessAccountEntity b where b.businessId = :businessId")
    java.util.Optional<BusinessAccountEntity> findForUpdate(String businessId);
}
