package com.inquiro.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessMembershipJpaRepository extends JpaRepository<BusinessMembershipEntity, String> {
    Optional<BusinessMembershipEntity> findByUserIdAndBusinessId(String userId, String businessId);
    List<BusinessMembershipEntity> findByBusinessId(String businessId);
}
