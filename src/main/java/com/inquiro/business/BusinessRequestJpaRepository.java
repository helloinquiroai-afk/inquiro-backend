package com.inquiro.business;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessRequestJpaRepository
        extends JpaRepository<BusinessRequestEntity, String> {

    List<BusinessRequestEntity> findByBusinessId(
            String businessId);

    List<BusinessRequestEntity> findByBusinessIdAndStatusIn(
            String businessId,
            List<BusinessRequestStatus> statuses);
}