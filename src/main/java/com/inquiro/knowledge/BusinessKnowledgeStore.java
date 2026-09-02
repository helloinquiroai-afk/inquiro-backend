package com.inquiro.knowledge;

import com.inquiro.business.BusinessProfile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class BusinessKnowledgeStore {

    private final Map<String, BusinessProfile> profiles =
            new ConcurrentHashMap<>();

    public void save(
            String businessId,
            BusinessProfile profile) {

        profiles.put(
                businessId,
                profile
        );
    }

    public BusinessProfile findByBusinessId(
            String businessId) {

        return profiles.get(businessId);
    }
}
