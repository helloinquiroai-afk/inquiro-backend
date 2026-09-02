package com.inquiro.knowledge;

import java.util.Map;

public record KnowledgeIngestionRequest(
        String businessId,
        String facebookPageId,
        KnowledgeSource source,
        String content,
        Map<String, Object> metadata
) {
}
