package com.inquiro.inquiry;

import com.inquiro.ai.AiService;
import com.inquiro.ai.RequestAnalysis;
import com.inquiro.ai.RequestAnalysisValidator;
import com.inquiro.availability.AvailabilityResult;
import com.inquiro.availability.AvailabilityService;
import com.inquiro.availability.AvailabilityStatus;
import com.inquiro.business.BusinessProfile;
import com.inquiro.business.BusinessProfileProvider;
import com.inquiro.business.BusinessQuestionService;
import com.inquiro.request.RequestDefinition;
import com.inquiro.request.SlotFillingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InquiryOrchestrator {

    private final AiService aiService;
    private final SlotFillingEngine slotFillingEngine;
    private final RequestAnalysisValidator validator;
    private final BusinessProfileProvider businessProfileProvider;
    private final BusinessQuestionService businessQuestionService;
    private final AvailabilityService availabilityService;

    public InquiryResponse process(String message) {

        /*
         * =========================================================
         * 1. LOAD BUSINESS PROFILE
         * =========================================================
         */

        BusinessProfile businessProfile =
                businessProfileProvider.get();

        return process(
                message,
                businessProfile
        );
    }

    public InquiryResponse process(
            String message,
            BusinessProfile businessProfile) {

        /*
         * =========================================================
         * 2. ANALYZE CUSTOMER REQUEST
         * =========================================================
         */

        RequestAnalysis analysis =
                aiService.analyzeRequest(
                        message,
                        businessProfile
                );

        /*
         * =========================================================
         * 3. GREETING
         * =========================================================
         *
         * Greetings are conversational.
         *
         * They do not require:
         * - slot filling
         * - availability checking
         * - business knowledge lookup
         */

        if ("GREETING".equalsIgnoreCase(
                analysis.intent())) {

            InquiryResult inquiry =
                    new InquiryResult(
                            businessProfile.businessType(),
                            analysis.intent(),
                            analysis.entities()
                    );

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    "Hi! Welcome to "
                            + businessProfile.businessName()
                            + ". How can I help you today?"
            );
        }

        /*
         * =========================================================
         * 4. BUSINESS QUESTION
         * =========================================================
         *
         * Examples:
         *
         * "What time is check-in?"
         * "Do you offer this service?"
         * "What facilities do you have?"
         *
         * These questions are answered from the business
         * owner's knowledge.
         *
         * They do NOT go through slot filling or availability
         * checking.
         */

        if ("BUSINESS_QUESTION".equalsIgnoreCase(
                analysis.intent())) {

            System.out.println(
                    "BUSINESS QUESTION"
            );

            String answer =
                    businessQuestionService.answer(
                            message,
                            businessProfile
                    );

            InquiryResult inquiry =
                    new InquiryResult(
                            businessProfile.businessType(),
                            analysis.intent(),
                            analysis.entities()
                    );

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    answer
            );
        }

        /*
         * =========================================================
         * 5. REQUEST NOT CLEAR
         * =========================================================
         *
         * Example:
         *
         * "I need a booking."
         *
         * The AI could not determine the actual service.
         */

        if (validator.needsClarification(analysis)) {

            return new InquiryResponse(
                    new InquiryResult(
                            "UNKNOWN",
                            "UNKNOWN",
                            Map.of()
                    ),
                    List.of(),
                    InquiryStatus.NEEDS_CLARIFICATION,
                    buildBusinessClarificationReply(
                            businessProfile
                    )
            );
        }

        /*
         * =========================================================
         * 6. FIND MISSING REQUIRED INFORMATION
         * =========================================================
         */

        List<String> missing =
                slotFillingEngine.findMissingSlots(
                        analysis,
                        businessProfile
                );

        /*
         * =========================================================
         * 7. CREATE INQUIRY RESULT
         * =========================================================
         */

        InquiryResult inquiry =
                new InquiryResult(
                        businessProfile.businessType(),
                        analysis.intent(),
                        analysis.entities()
                );

        /*
         * =========================================================
         * 8. INFORMATION STILL MISSING
         * =========================================================
         *
         * Do NOT check availability yet.
         *
         * First collect all information required for the
         * requested service.
         */

        if (!missing.isEmpty()) {

            return new InquiryResponse(
                    inquiry,
                    missing,
                    InquiryStatus.NEEDS_INFORMATION,
                    buildReply(
                            missing,
                            businessProfile,
                            analysis.intent()
                    )
            );
        }

        /*
         * =========================================================
         * 9. ALL REQUIRED INFORMATION COLLECTED
         * =========================================================
         *
         * Now we can ask the availability layer.
         *
         * IMPORTANT:
         *
         * AvailabilityService is responsible for deciding
         * whether the available information is:
         *
         * CONFIRMED
         * INDICATED
         * UNAVAILABLE
         * UNKNOWN
         *
         * Inquiro itself does not assume availability.
         */

        System.out.println(
                "ALL REQUIRED INFORMATION COLLECTED"
        );

        AvailabilityResult availability =
                availabilityService.checkAvailability(
                        analysis.intent(),
                        analysis.entities(),
                        businessProfile
                );

        /*
         * =========================================================
         * 10. BUILD AVAILABILITY RESPONSE
         * =========================================================
         */

        return buildAvailabilityResponse(
                inquiry,
                availability
        );
    }

    /*
     * =============================================================
     * BUILD AVAILABILITY RESPONSE
     * =============================================================
     *
     * The important distinction is:
     *
     * CONFIRMED
     *     The source is reliable enough to confirm availability.
     *
     * INDICATED
     *     Business information suggests availability, but the
     *     business must still confirm it.
     *
     * UNAVAILABLE
     *     A reliable source says it is unavailable.
     *
     * UNKNOWN
     *     We cannot determine availability.
     */

    private InquiryResponse buildAvailabilityResponse(
            InquiryResult inquiry,
            AvailabilityResult availability) {

        /*
         * ---------------------------------------------------------
         * No availability result
         * ---------------------------------------------------------
         */

        if (availability == null) {

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    buildBusinessConfirmationMessage()
            );
        }

        /*
         * ---------------------------------------------------------
         * CONFIRMED
         * ---------------------------------------------------------
         */

        if (availability.status() ==
                AvailabilityStatus.CONFIRMED) {

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    availability.message()
            );
        }

        /*
         * ---------------------------------------------------------
         * INDICATED
         * ---------------------------------------------------------
         *
         * We have an indication from business information,
         * but we do NOT promise availability.
         */

        if (availability.status() ==
                AvailabilityStatus.INDICATED) {

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    buildIndicatedAvailabilityMessage(
                            availability.message()
                    )
            );
        }

        /*
         * ---------------------------------------------------------
         * UNAVAILABLE
         * ---------------------------------------------------------
         */

        if (availability.status() ==
                AvailabilityStatus.UNAVAILABLE) {

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    availability.message()
            );
        }

        /*
         * ---------------------------------------------------------
         * UNKNOWN
         * ---------------------------------------------------------
         *
         * We don't have enough reliable information to determine
         * availability.
         *
         * Therefore we collect the request and let the business
         * confirm it.
         */

        return new InquiryResponse(
                inquiry,
                List.of(),
                InquiryStatus.INFORMATION_COLLECTED,
                buildBusinessConfirmationMessage()
        );
    }

    /*
     * =============================================================
     * INDICATED AVAILABILITY MESSAGE
     * =============================================================
     */

    private String buildIndicatedAvailabilityMessage(
            String availabilityMessage) {

        if (availabilityMessage == null ||
                availabilityMessage.isBlank()) {

            return buildBusinessConfirmationMessage();
        }

        return availabilityMessage
                + " However, final availability needs to be "
                + "confirmed by the business. "
                + "We will contact you as soon as possible.";
    }

    /*
     * =============================================================
     * BUSINESS CONFIRMATION MESSAGE
     * =============================================================
     *
     * This deliberately does not mention a specific business name.
     *
     * The same message can therefore be used for:
     *
     * - hotels
     * - restaurants
     * - healthcare businesses
     * - future business types
     */

    private String buildBusinessConfirmationMessage() {

        return "Thank you. We have received your request. "
                + "The business will confirm availability "
                + "and contact you as soon as possible.";
    }

    /*
     * =============================================================
     * BUSINESS-BASED CLARIFICATION
     * =============================================================
     */

    private String buildBusinessClarificationReply(
            BusinessProfile businessProfile) {

        List<String> services =
                businessProfile.knowledge().services();

        if (services == null || services.isEmpty()) {

            return "How can I help you?";
        }

        return "I can currently help with "
                + String.join(
                " and ",
                services
        )
                + ". How can I help?";
    }

    /*
     * =============================================================
     * BUILD FOLLOW-UP QUESTION
     * =============================================================
     */

    private String buildReply(
            List<String> missingFields,
            BusinessProfile businessProfile,
            String service) {

        String field =
                missingFields.get(0);

        RequestDefinition definition =
                businessProfile.services()
                        .stream()
                        .filter(candidate ->
                                candidate.requestType()
                                        .equalsIgnoreCase(service))
                        .findFirst()
                        .orElse(null);

        if (definition != null &&
                definition.slotPrompts().containsKey(field)) {

            return definition.slotPrompts()
                    .get(field);
        }

        return "Could you please provide "
                + field.replaceAll(
                "([a-z])([A-Z])",
                "$1 $2"
        ).toLowerCase()
                + "?";
    }
}
