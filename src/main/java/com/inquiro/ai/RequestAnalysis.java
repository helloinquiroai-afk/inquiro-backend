package com.inquiro.ai;

import java.util.Map;

public record RequestAnalysis(
        String intent,
        Double confidence,
        Map<String, Object> entities,
        java.util.List<String> knowledgeQuestions
) {
    public RequestAnalysis(String intent, Double confidence, Map<String, Object> entities) {
        this(intent, confidence, entities, java.util.List.of());
    }

    public RequestAnalysis {
        entities = entities == null ? Map.of() : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(entities));
        knowledgeQuestions = questions(knowledgeQuestions);
    }

    static java.util.List<String> questions(java.util.List<String> values) {
        if (values == null) return java.util.List.of();
        if (values.size() > 3 || values.stream().anyMatch(value -> value == null || value.isBlank() || value.length() > 2000)) {
            throw new IllegalArgumentException("Invalid knowledge questions");
        }
        return java.util.List.copyOf(values);
    }
}
