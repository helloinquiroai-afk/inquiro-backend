package com.inquiro.ai;

import java.util.Map;

public record RequestAnalysis(
        String domain,
        String requestType,
        Double confidence,
        Map<String,Object> entities
) {
}
