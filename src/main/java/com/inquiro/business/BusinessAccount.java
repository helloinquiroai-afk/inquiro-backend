package com.inquiro.business;

public record BusinessAccount(
        String businessId,
        String businessName,
        String facebookPageId,
        BusinessProfile profile
) {
}
