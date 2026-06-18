package com.inquiro.ai;

import java.util.Map;

public record RequestAnalysis(

        String requestType,

        String domain,

        Map<String, Object> entities

) {
}
