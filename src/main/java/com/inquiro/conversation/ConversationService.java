package com.inquiro.conversation;

import com.inquiro.ai.AiService;
import com.inquiro.ai.ConversationIntentAnalysis;
import com.inquiro.ai.FollowUpAnalysis;
import com.inquiro.ai.RequestAnalysis;
import com.inquiro.availability.AvailabilityResult;
import com.inquiro.availability.AvailabilityService;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessBoundaryService;
import com.inquiro.business.BusinessProfile;
import com.inquiro.business.BusinessRequest;
import com.inquiro.business.BusinessRequestService;
import com.inquiro.inquiry.InquiryOrchestrator;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;
import com.inquiro.inquiry.InquiryStatus;
import com.inquiro.request.RequestDefinition;
import com.inquiro.request.SlotFillingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationStore conversationStore;
    private final InquiryOrchestrator inquiryOrchestrator;
    private final AiService aiService;
    private final SlotFillingEngine slotFillingEngine;
    private final BusinessAccountRepository businessAccountRepository;
    private final AvailabilityService availabilityService;
    private final BusinessRequestService businessRequestService;
    private final BusinessBoundaryService businessBoundaryService;

    public InquiryResponse process(
            String sessionId,
            String facebookPageId,
            String message) {

        System.out.println(
                "\n=================================================="
        );

        System.out.println(
                "ConversationService.process()"
        );

        System.out.println(
                "Session ID : " + sessionId
        );

        System.out.println(
                "Facebook Page ID : " + facebookPageId
        );

        System.out.println(
                "Message    : " + message
        );

        System.out.println(
                "=================================================="
        );

        /*
         * =========================================================
         * 1. RESOLVE BUSINESS
         * =========================================================
         */

        BusinessAccount businessAccount =
                businessAccountRepository.findByFacebookPageId(
                        facebookPageId
                );

        if (businessAccount == null) {

            throw new IllegalStateException(
                    "No business configured for Facebook Page: "
                            + facebookPageId
            );
        }

        BusinessProfile businessProfile =
                businessAccount.profile();

        System.out.println(
                "Business    : "
                        + businessAccount.businessName()
        );

        System.out.println(
                "Business ID : "
                        + businessAccount.businessId()
        );

        /*
         * =========================================================
         * 2. LOAD CONVERSATION
         * =========================================================
         */

        ConversationSession session =
                conversationStore.get(sessionId);

        /*
         * =========================================================
         * 3. NEW CONVERSATION
         * =========================================================
         */

        if (session == null) {

            System.out.println("NEW CONVERSATION");

            InquiryResponse response =
                    inquiryOrchestrator.process(
                            message,
                            businessProfile
                    );

            printResponse(response);

            /*
             * =====================================================
             * REQUEST NEEDS MORE INFORMATION
             * =====================================================
             */

            if (response.status()
                    == InquiryStatus.NEEDS_INFORMATION) {

                saveConversation(
                        sessionId,
                        response
                );

                return response;
            }

            /*
             * =====================================================
             * REQUEST IS ALREADY COMPLETE
             * =====================================================
             */

            if (response.status()
                    == InquiryStatus.INFORMATION_COLLECTED
                    && isBusinessRequest(response.inquiry())) {

                return processCompletedRequest(
                        businessAccount,
                        sessionId,
                        response.inquiry()
                );
            }

            return response;
        }

        /*
         * =========================================================
         * 4. EXISTING CONVERSATION
         * =========================================================
         */

        System.out.println(
                "EXISTING CONVERSATION"
        );

        System.out.println(
                "Stored Inquiry      : "
                        + session.getInquiry()
        );

        System.out.println(
                "Stored Fields       : "
                        + session.getInquiry().fields()
        );

        System.out.println(
                "Stored Missing      : "
                        + session.getMissingFields()
        );

        /*
         * =========================================================
         * 5. DETERMINE FOLLOW-UP OR NEW REQUEST
         * =========================================================
         */

        ConversationIntentAnalysis intent =
                aiService.analyzeConversationIntent(
                        businessProfile,
                        session.getInquiry().service(),
                        session.getInquiry().fields(),
                        session.getMissingFields(),
                        message
                );

        System.out.println(
                "Conversation Intent : "
                        + intent.intent()
        );

        System.out.println(
                "Intent Confidence  : "
                        + intent.confidence()
        );

        /*
         * =========================================================
         * 6. NEW REQUEST
         * =========================================================
         */

        if ("NEW_REQUEST".equalsIgnoreCase(
                intent.intent())) {

            System.out.println(
                    "CUSTOMER STARTED A NEW REQUEST"
            );

            conversationStore.remove(
                    sessionId
            );

            InquiryResponse response =
                    inquiryOrchestrator.process(
                            message,
                            businessProfile
                    );

            printResponse(response);

            /*
             * Save incomplete new request.
             */

            if (response.status()
                    == InquiryStatus.NEEDS_INFORMATION) {

                saveConversation(
                        sessionId,
                        response
                );

                return response;
            }

            /*
             * Process complete new request.
             */

            if (response.status()
                    == InquiryStatus.INFORMATION_COLLECTED
                    && isBusinessRequest(response.inquiry())) {

                return processCompletedRequest(
                        businessAccount,
                        sessionId,
                        response.inquiry()
                );
            }

            return response;
        }

        /*
         * =========================================================
         * 7. FOLLOW-UP TO EXISTING REQUEST
         * =========================================================
         */

        System.out.println(
                "CUSTOMER CONTINUES EXISTING REQUEST"
        );

        FollowUpAnalysis replyAnalysis =
                aiService.analyzeFollowUp(
                        session.getInquiry().service(),
                        session.getInquiry().fields(),
                        session.getMissingFields(),
                        message
                );

        System.out.println(
                "AI Follow-up Entities : "
                        + replyAnalysis.entities()
        );

        /*
         * =========================================================
         * 8. MERGE CUSTOMER INFORMATION
         * =========================================================
         */

        Map<String, Object> fields =
                EntityMerger.merge(
                        session.getInquiry().fields(),
                        replyAnalysis.entities()
                );

        System.out.println(
                "Merged Fields : "
                        + fields
        );

        RequestAnalysis updatedAnalysis =
                new RequestAnalysis(
                        session.getInquiry().service(),
                        1.0,
                        fields
                );

        InquiryResult updatedInquiry =
                new InquiryResult(
                        session.getInquiry().domain(),
                        session.getInquiry().service(),
                        fields
                );

        /*
         * =========================================================
         * 9. CHECK REQUIRED INFORMATION
         * =========================================================
         */

        List<String> missing =
                slotFillingEngine.findMissingSlots(
                        updatedAnalysis,
                        businessProfile
                );

        System.out.println(
                "Missing After Merge : "
                        + missing
        );

        /*
         * =========================================================
         * 10. STILL MISSING INFORMATION
         * =========================================================
         */

        if (!missing.isEmpty()) {

            ConversationSession updatedSession =
                    new ConversationSession(
                            sessionId,
                            updatedInquiry,
                            missing,
                            Instant.now()
                    );

            System.out.println(
                    "Saving Updated Conversation"
            );

            conversationStore.save(
                    updatedSession
            );

            String reply =
                    buildReply(
                            missing,
                            businessProfile,
                            updatedInquiry.service()
                    );

            System.out.println(
                    "Reply : " + reply
            );

            return new InquiryResponse(
                    updatedInquiry,
                    missing,
                    InquiryStatus.NEEDS_INFORMATION,
                    reply
            );
        }

        /*
         * =========================================================
         * 11. ALL INFORMATION COLLECTED
         * =========================================================
         */

        System.out.println(
                "Conversation Complete"
        );

        return processCompletedRequest(
                businessAccount,
                sessionId,
                updatedInquiry
        );
    }

    /*
     * =============================================================
     * PROCESS COMPLETED BUSINESS REQUEST
     * =============================================================
     *
     * Flow:
     *
     * 1. Validate business boundary
     * 2. Check availability
     * 3. Create business request
     * 4. Notify customer
     *
     */

    private InquiryResponse processCompletedRequest(
            BusinessAccount businessAccount,
            String customerId,
            InquiryResult inquiry) {

        if (!isBusinessRequest(inquiry)) {

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    "Thank you."
            );
        }

        /*
         * =========================================================
         * 1. CHECK BUSINESS BOUNDARY
         * =========================================================
         *
         * This is intentionally BEFORE availability.
         *
         * Availability should only be checked for services
         * that the business actually supports.
         */

        BusinessBoundaryService.BoundaryResult boundary =
                businessBoundaryService.check(
                        inquiry.service(),
                        businessAccount.profile()
                );

        System.out.println(
                "Business Boundary Status : "
                        + boundary.status()
        );

        if (boundary.message() != null) {

            System.out.println(
                    "Business Boundary Message : "
                            + boundary.message()
            );
        }

        /*
         * =========================================================
         * REQUEST OUTSIDE BUSINESS BOUNDARY
         * =========================================================
         */

        if (boundary.status()
                != BusinessBoundaryService.BoundaryStatus.SUPPORTED) {

            /*
             * The customer request is complete, but we must not
             * create a BusinessRequest because the business has
             * not declared this service as supported.
             */

            conversationStore.remove(
                    customerId
            );

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    boundary.message()
            );
        }

        /*
         * =========================================================
         * 2. CHECK CURRENT AVAILABILITY INFORMATION
         * =========================================================
         */

        AvailabilityResult availability =
                availabilityService.checkAvailability(
                        inquiry.service(),
                        inquiry.fields(),
                        businessAccount.profile()
                );

        System.out.println(
                "Availability Status : "
                        + availability.status()
        );

        System.out.println(
                "Availability Message : "
                        + availability.message()
        );

        /*
         * =========================================================
         * 3. CREATE BUSINESS REQUEST
         * =========================================================
         *
         * BusinessRequestService decides whether the request
         * should be PENDING_CONFIRMATION, CONFIRMED, etc.
         */

        BusinessRequest businessRequest =
                businessRequestService.create(
                        businessAccount.businessId(),
                        customerId,
                        inquiry.service(),
                        inquiry.fields(),
                        availability.status()
                );

        System.out.println(
                "Business Request Created:"
        );

        System.out.println(
                businessRequest
        );

        /*
         * =========================================================
         * 4. CONVERSATION IS COMPLETE
         * =========================================================
         */

        conversationStore.remove(
                customerId
        );

        /*
         * =========================================================
         * 5. CUSTOMER RESPONSE
         * =========================================================
         */

        return buildAvailabilityResponse(
                inquiry,
                availability
        );
    }

    /*
     * =============================================================
     * SAVE CONVERSATION
     * =============================================================
     */

    private void saveConversation(
            String sessionId,
            InquiryResponse response) {

        System.out.println(
                "Saving conversation..."
        );

        conversationStore.save(
                new ConversationSession(
                        sessionId,
                        response.inquiry(),
                        response.missingFields(),
                        Instant.now()
                )
        );
    }

    /*
     * =============================================================
     * DETERMINE BUSINESS REQUEST
     * =============================================================
     */

    private boolean isBusinessRequest(
            InquiryResult inquiry) {

        if (inquiry == null) {
            return false;
        }

        String service =
                inquiry.service();

        if (service == null ||
                service.isBlank()) {

            return false;
        }

        return !"UNKNOWN".equalsIgnoreCase(service)
                && !"GREETING".equalsIgnoreCase(service)
                && !"BUSINESS_QUESTION".equalsIgnoreCase(service);
    }

    /*
     * =============================================================
     * BUILD AVAILABILITY RESPONSE
     * =============================================================
     */

    private InquiryResponse buildAvailabilityResponse(
            InquiryResult inquiry,
            AvailabilityResult availability) {

        if (availability == null) {

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    buildPendingMessage()
            );
        }

        return switch (availability.status()) {

            case CONFIRMED ->

                    new InquiryResponse(
                            inquiry,
                            List.of(),
                            InquiryStatus.INFORMATION_COLLECTED,
                            availability.message()
                    );

            case INDICATED ->

                    new InquiryResponse(
                            inquiry,
                            List.of(),
                            InquiryStatus.INFORMATION_COLLECTED,
                            availability.message()
                    );

            case UNAVAILABLE ->

                    new InquiryResponse(
                            inquiry,
                            List.of(),
                            InquiryStatus.INFORMATION_COLLECTED,
                            availability.message()
                    );

            case UNKNOWN ->

                    new InquiryResponse(
                            inquiry,
                            List.of(),
                            InquiryStatus.INFORMATION_COLLECTED,
                            availability.message()
                    );
        };
    }

    /*
     * =============================================================
     * DEFAULT PENDING MESSAGE
     * =============================================================
     */

    private String buildPendingMessage() {

        return "Thank you. We have received your request. "
                + "The business will confirm availability "
                + "and contact you shortly.";
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

    /*
     * =============================================================
     * DEBUG RESPONSE
     * =============================================================
     */

    private void printResponse(
            InquiryResponse response) {

        System.out.println(
                "AI Inquiry      : "
                        + response.inquiry()
        );

        System.out.println(
                "Missing Fields  : "
                        + response.missingFields()
        );

        System.out.println(
                "Status          : "
                        + response.status()
        );

        System.out.println(
                "Reply           : "
                        + response.reply()
        );
    }
}