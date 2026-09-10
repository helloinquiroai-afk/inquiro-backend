package com.inquiro.ai;

import org.springframework.stereotype.Component;

@Component
public class RequestAnalysisValidator {

    public boolean needsClarification(
            RequestAnalysis analysis) {

        return analysis == null || analysis.intent() == null || analysis.intent().isBlank()
                || "UNKNOWN".equalsIgnoreCase(analysis.intent()) || analysis.confidence() == null
                || analysis.confidence()
                < AiConstants.CONFIDENCE_THRESHOLD;
    }
}
