package com.inquiro.business;

public interface BusinessAccountRepository {

    BusinessAccount findByFacebookPageId(
            String facebookPageId);

    void save(BusinessAccount account);
}
