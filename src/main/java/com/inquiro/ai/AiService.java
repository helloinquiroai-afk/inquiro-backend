package com.inquiro.ai;

import com.inquiro.business.BusinessProfile;
import com.inquiro.inquiry.InquiryResult;

import java.util.List;
import java.util.Map;

public interface AiService {

    InquiryResult analyze(String message);

    RequestAnalysis analyzeRequest(String message);

    RequestAnalysis analyzeRequest(
            String message,
            BusinessProfile businessProfile
    );

    FollowUpAnalysis analyzeFollowUp(
            String service,
            Map<String, Object> fields,
            List<String> missingFields,
            String message
    );

    ConversationIntentAnalysis analyzeConversationIntent(
            String service,
            Map<String, Object> currentFields,
            List<String> missingFields,
            String message
    );

    ConversationIntentAnalysis analyzeConversationIntent(
            BusinessProfile businessProfile,
            String service,
            Map<String, Object> currentFields,
            List<String> missingFields,
            String message
    );

    String answerBusinessQuestion(
            String customerQuestion,
            BusinessProfile businessProfile
    );
}
