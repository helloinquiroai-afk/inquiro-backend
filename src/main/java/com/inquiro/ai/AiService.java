package com.inquiro.ai;

import com.inquiro.inquiry.InquiryResult;

import java.util.List;
import java.util.Map;

public interface AiService {
    InquiryResult analyze(String message);
    RequestAnalysis analyzeRequest(String message);

    FollowUpAnalysis analyzeFollowUp(String service, Map<String, Object> fields, List<String> missingFields, String message);
}
