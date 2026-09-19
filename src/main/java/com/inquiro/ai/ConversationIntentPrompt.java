package com.inquiro.ai;

import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;

public final class ConversationIntentPrompt {

    private ConversationIntentPrompt() {
    }

    public static String build(
            BusinessProfile businessProfile,
            String currentService,
            java.util.Map<String, Object> currentFields,
            java.util.List<String> missingFields,
            String message) {

        StringBuilder services = new StringBuilder();

        for (RequestDefinition service : businessProfile.services()) {
            services.append("""
                    
                    Service code:
                    %s
                    
                    Description:
                    %s
                    
                    Required fields:
                    %s
                    
                    """.formatted(
                    service.requestType(),
                    service.description(),
                    service.requiredSlots()
            ));
        }

        return """
                You are a conversation intent classifier.
                
                BUSINESS
                
                Name:
                %s
                
                Type:
                %s
                
                AVAILABLE SERVICES
                
                %s
                
                CURRENT CONVERSATION
                
                Current service:
                %s
                
                Current fields:
                %s
                
                Missing fields:
                %s
                
                Customer message:
                "%s"
                
                Classify the customer message as FOLLOW_UP, NEW_REQUEST, BUSINESS_QUESTION, or GENERAL_QUESTION.
                
                FOLLOW_UP means the customer is providing information related to the current service
                or answering one of the missing fields.
                
                NEW_REQUEST means the customer is clearly starting a different service request
                and not merely asking a question about the business.
                
                BUSINESS_QUESTION means a question about THIS business, its services, products,
                rules, prices, locations, availability, policies, facilities, or contact details.
                
                GENERAL_QUESTION means a general knowledge, educational, geographic, cultural,
                language, mathematical, technical, or everyday question that is NOT asking about
                this business. The receptionist may answer general questions using its general AI
                knowledge, like a general-purpose AI assistant.
                
                If a question could be interpreted as either business-specific or general, use
                BUSINESS_QUESTION when it refers to the business, and GENERAL_QUESTION otherwise.
                
                A message that provides details/corrections for the current workflow AND asks a
                business or general question is FOLLOW_UP. Include only the explicitly asked question
                in knowledgeQuestions and do not discard the supplied workflow details.
                
                Short answers such as a date, time, number, name, location, or contact detail are
                usually FOLLOW_UP when they plausibly fill a missing field.
                
                Return ONLY valid JSON:
                
                {
                  "intent": "FOLLOW_UP",
                  "confidence": 0.0,
                  "knowledgeQuestions": []
                }
                
                or:
                
                {
                  "intent": "GENERAL_QUESTION",
                  "confidence": 0.0,
                  "knowledgeQuestions": []
                }
                """
                .formatted(
                        businessProfile.businessName(),
                        businessProfile.businessType(),
                        services,
                        currentService,
                        currentFields,
                        missingFields,
                        message
                );
    }
}
