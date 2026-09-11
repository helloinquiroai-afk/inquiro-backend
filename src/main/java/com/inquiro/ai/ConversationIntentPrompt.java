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

        StringBuilder services =
                new StringBuilder();

        for (RequestDefinition service :
                businessProfile.services()) {

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

                Classify the customer message as FOLLOW_UP, NEW_REQUEST, or BUSINESS_QUESTION.

                FOLLOW_UP means the customer is providing information related to the current service
                or answering one of the missing fields.

                NEW_REQUEST means the customer is clearly starting a different service request
                and not merely asking a question about the business.

                BUSINESS_QUESTION means a knowledge-only interruption, such as asking about parking,
                hours, prices or policies, without supplying workflow details.
                A message that provides details/corrections for the current workflow AND asks a business
                question is FOLLOW_UP. Include its question(s) in knowledgeQuestions and do not discard
                the supplied details. A different workflow plus a question is NEW_REQUEST.
                knowledgeQuestions must contain only explicitly asked, self-contained questions,
                up to 3 questions of at most 2000 characters each; otherwise use an empty array.

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
                  "intent": "NEW_REQUEST",
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
