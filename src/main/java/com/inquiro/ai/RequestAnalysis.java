package com.inquiro.ai;

import java.util.Map;

public record RequestAnalysis(
        String intent,
        Double confidence,
        Map<String, Object> entities
) {
}
