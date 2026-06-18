package com.inquiro.ai;

import com.inquiro.inquiry.InquiryResult;

public interface AiService {
    InquiryResult analyze(String message);
    RequestAnalysis analyzeRequest(String message);
}
