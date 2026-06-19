package com.inquiro.ai;

import org.springframework.stereotype.Component;

@Component
public class RequestAnalysisValidator {

    public boolean needsClarification(
            RequestAnalysis analysis) {

        return analysis.confidence() == null
                || analysis.confidence()
                < AiConstants.CONFIDENCE_THRESHOLD;
    }
}
