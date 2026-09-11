package com.inquiro.business;

import com.inquiro.request.RequestDefinition;

import java.util.List;

public record BusinessProfile(

        String businessName,

        String businessType,

        String description,

        List<RequestDefinition> services,

        BusinessKnowledge knowledge

) {
    public BusinessProfile {
        services = services == null ? List.of() : List.copyOf(services);
        knowledge = knowledge == null ? BusinessKnowledge.empty() : knowledge;
    }

    public BusinessProfile withKnowledge(BusinessKnowledge replacement) {
        return new BusinessProfile(businessName, businessType, description, services, replacement);
    }
}
