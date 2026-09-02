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
}
