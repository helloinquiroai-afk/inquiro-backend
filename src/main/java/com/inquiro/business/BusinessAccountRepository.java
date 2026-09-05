package com.inquiro.business;

public interface BusinessAccountRepository {

    BusinessAccount findByBusinessId(
            String businessId
    );

    void save(
            BusinessAccount account
    );
}
