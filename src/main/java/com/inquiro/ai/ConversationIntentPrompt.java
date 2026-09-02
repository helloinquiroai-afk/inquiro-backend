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

                Classify the customer message as FOLLOW_UP or NEW_REQUEST.

                FOLLOW_UP means the customer is providing information related to the current service
                or answering one of the missing fields.

                NEW_REQUEST means the customer is clearly starting a different service request
                or asking for a different business capability.

                Short answers such as a date, time, number, name, location, or contact detail are
                usually FOLLOW_UP when they plausibly fill a missing field.

                Return ONLY valid JSON:

                {
                  "intent": "FOLLOW_UP",
                  "confidence": 0.0
                }

                or:

                {
                  "intent": "NEW_REQUEST",
                  "confidence": 0.0
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
