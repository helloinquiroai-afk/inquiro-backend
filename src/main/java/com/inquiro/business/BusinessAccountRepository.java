package com.inquiro.business;

public interface BusinessAccountRepository {

    BusinessAccount findByBusinessId(
            String businessId
    );

    default BusinessAccount findByBusinessIdForUpdate(String businessId) {
        return findByBusinessId(businessId);
    }

    void save(
            BusinessAccount account
    );
}
