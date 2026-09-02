package com.inquiro.ai;

import com.inquiro.business.BusinessProfile;

public final class RequestAnalysisPrompt {

    private RequestAnalysisPrompt() {
    }

    public static String systemPrompt(
            BusinessProfile businessProfile) {

        StringBuilder services =
                new StringBuilder();

        businessProfile.services()
                .forEach(service ->
                        services.append("""

                                Service code:
                                %s

                                Description:
                                %s

                                Required fields:
                                %s

                                Allowed field names:
                                %s

                                """.formatted(
                                service.requestType(),
                                service.description(),
                                service.requiredSlots(),
                                service.requiredSlots()
                        ))
                );

        return """
                You are the customer request understanding engine for a business.

                BUSINESS

                Name:
                %s

                Type:
                %s

                Description:
                %s

                CONFIGURED SERVICES

                %s

                BUSINESS BOUNDARIES

                Supported:
                %s

                Not supported:
                %s

                Requires business review:
                %s

                TASK

                Identify the customer's intent and extract only explicitly provided useful entities.

                If the customer wants to use, book, buy, arrange, or request one configured service,
                return that configured service code exactly.

                If the customer asks a question about the business, its services, products, rules,
                prices, locations, availability, policies, or contact details, return BUSINESS_QUESTION.

                If the customer only greets the business, return GREETING.

                If the customer request is unclear, return UNKNOWN.

                Do not answer the customer. Only classify and extract.
                Do not invent fields or values.
                For configured services, prefer the required field names listed above.
                If the customer provides useful information that does not map to a required field,
                use a concise camelCase field name.

                Return ONLY valid JSON in this format:

                {
                  "intent": "",
                  "confidence": 0.0,
                  "entities": {}
                }
                """
                .formatted(
                        businessProfile.businessName(),
                        businessProfile.businessType(),
                        businessProfile.description(),
                        services,
                        businessProfile.knowledge().boundaries().supported(),
                        businessProfile.knowledge().boundaries().notSupported(),
                        businessProfile.knowledge().boundaries().requiresHuman()
                );
    }
}
