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
                
                SERVICE CLASSIFICATION RULES
                
                1. If the customer wants to use, book, buy, arrange, or request one configured service,
                   return that configured service code exactly.
                
                2. If the customer requests a service that appears under BUSINESS BOUNDARIES ->
                   Not supported, classify the request using a normalized service code derived from that
                   boundary entry. Normalize by trimming, converting to uppercase, and replacing spaces
                   and hyphens with underscores.
                
                   Example:
                   "Engine replacement" -> "ENGINE_REPLACEMENT"
                   "Body painting" -> "BODY_PAINTING"
                
                3. If the customer requests a service that appears under BUSINESS BOUNDARIES ->
                   Requires business review, classify it using the same normalized service code.
                
                4. Do not return UNKNOWN when the requested service clearly matches a configured service,
                   a not-supported boundary entry, or a requires-human boundary entry.
                
                5. Return UNKNOWN only when the requested service cannot be reasonably matched to any
                   configured service or business-boundary entry, or when the request itself is unclear.
                
                BUSINESS QUESTIONS
                
                If the customer asks a question about the business, its services, products, rules,
                prices, locations, availability, policies, or contact details, return BUSINESS_QUESTION.
                
                GREETING
                
                If the customer only greets the business, return GREETING.
                
                ENTITY EXTRACTION
                
                Extract only explicitly provided useful entities.
                Do not invent fields or values.
                For configured services, prefer the required field names listed above.
                If the customer provides useful information that does not map to a required field,
                use a concise camelCase field name.
                
                Do not answer the customer. Only classify and extract.
                
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