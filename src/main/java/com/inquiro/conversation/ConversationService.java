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
import com.inquiro.business.BusinessChannel;
import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessChannelType;
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

    private final ConversationRepository conversationRepository;

    private final InquiryOrchestrator inquiryOrchestrator;

    private final AiService aiService;

    private final SlotFillingEngine slotFillingEngine;

    private final BusinessAccountRepository businessAccountRepository;

    private final BusinessChannelRepository businessChannelRepository;

    private final AvailabilityService availabilityService;

    private final BusinessRequestService businessRequestService;

    private final BusinessBoundaryService businessBoundaryService;


    public InquiryResponse process(
            String sessionId,
            String externalChannelId,
            String message) {

        return process(sessionId, BusinessChannelType.MESSENGER, externalChannelId, message);
    }

    public InquiryResponse process(String sessionId, BusinessChannelType channelType,
                                   String externalChannelId, String message) {


        /*
         * =========================================================
         * 1. RESOLVE CHANNEL
         * =========================================================
         *
         * The external platform identifier belongs to a channel.
         *
         * It does NOT directly identify the business.
         *
         * Example:
         *
         * Messenger Page ID
         *        ↓
         * BusinessChannel
         *        ↓
         * businessId
         */

        BusinessChannel channel =
                businessChannelRepository
                        .findByTypeAndExternalId(
                                channelType,
                                externalChannelId
                        );

        if (channel == null) {

            throw new IllegalStateException(
                    "No business channel configured for external ID: "
                            + externalChannelId
            );
        }

        if (!channel.enabled()) {

            throw new IllegalStateException(
                    "Business channel is disabled: "
                            + externalChannelId
            );
        }


/*
         * =========================================================
         * 2. RESOLVE BUSINESS
         * =========================================================
         *
         * The business is resolved independently of the channel.
         */

        BusinessAccount businessAccount =
                businessAccountRepository.findByBusinessId(
                        channel.businessId()
                );

        if (businessAccount == null) {

            throw new IllegalStateException(
                    "No business configured for business ID: "
                            + channel.businessId()
            );
        }

        BusinessProfile businessProfile =
                businessAccount.profile();

        sessionId = new ConversationIdentity(businessAccount.businessId(), channelType,
                externalChannelId, sessionId).sessionId();


        /*
         * =========================================================
         * 3. LOAD CONVERSATION
         * =========================================================
         */

        ConversationSession session =
                conversationRepository.find(sessionId);


        /*
         * =========================================================
         * 4. NEW CONVERSATION
         * =========================================================
         */

        if (session == null) {

InquiryResponse response =
                    inquiryOrchestrator.process(
                            message,
                            businessProfile
                    );


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
                    && isBusinessRequest(
                    response.inquiry()
            )) {

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
         * 5. EXISTING CONVERSATION
         * =========================================================
         */


        /*
         * =========================================================
         * 6. DETERMINE FOLLOW-UP OR NEW REQUEST
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


        /*
         * =========================================================
         * 7. NEW REQUEST
         * =========================================================
         */

        if ("NEW_REQUEST".equalsIgnoreCase(
                intent.intent()
        )) {

            /*
             * Analyze the message as a completely
             * new request using the current business profile.
             */

            InquiryResponse response =
                    inquiryOrchestrator.process(
                            message,
                            businessProfile
                    );


            // Keep unfinished requests when answering questions or asking for clarification.
            if (!isBusinessRequest(response.inquiry())) return response;
            conversationRepository.remove(sessionId);

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
                    && isBusinessRequest(
                    response.inquiry()
            )) {

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
         * 8. FOLLOW-UP TO EXISTING REQUEST
         * =========================================================
         */

FollowUpAnalysis replyAnalysis =
                aiService.analyzeFollowUp(
                        session.getInquiry().service(),
                        session.getInquiry().fields(),
                        session.getMissingFields(),
                        message
                );

/*
         * =========================================================
         * 9. MERGE CUSTOMER INFORMATION
         * =========================================================
         */

        Map<String, Object> fields =
                EntityMerger.merge(
                        session.getInquiry().fields(),
                        replyAnalysis.entities()
                );

/*
         * =========================================================
         * 10. BUILD UPDATED REQUEST ANALYSIS
         * =========================================================
         */

        RequestAnalysis updatedAnalysis =
                new RequestAnalysis(
                        session.getInquiry().service(),
                        1.0,
                        fields
                );


        /*
         * =========================================================
         * 11. BUILD UPDATED INQUIRY
         * =========================================================
         */

        InquiryResult updatedInquiry =
                new InquiryResult(
                        session.getInquiry().domain(),
                        session.getInquiry().service(),
                        fields
                );


        /*
         * =========================================================
         * 12. CHECK REQUIRED INFORMATION
         * =========================================================
         */

        List<String> missing =
                slotFillingEngine.findMissingSlots(
                        updatedAnalysis,
                        businessProfile
                );

/*
         * =========================================================
         * 13. STILL MISSING INFORMATION
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

conversationRepository.save(
                    updatedSession
            );

            String reply =
                    buildReply(
                            missing,
                            businessProfile,
                            updatedInquiry.service()
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
         * 14. ALL INFORMATION COLLECTED
         * =========================================================
         */

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
     * 2. If human review required → create review request
     * 3. If unsupported → stop
     * 4. Check availability
     * 5. Create business request
     * 6. Clear conversation
     * 7. Return customer response
     *
     */

    private InquiryResponse processCompletedRequest(
            BusinessAccount businessAccount,
            String customerId,
            InquiryResult inquiry) {


        /*
         * =========================================================
         * NOT A BUSINESS REQUEST
         * =========================================================
         */

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
         * This must happen BEFORE availability.
         *
         * Availability should only be checked for services
         * supported by the business.
         */

        BusinessBoundaryService.BoundaryResult boundary =
                businessBoundaryService.check(
                        inquiry.service(),
                        businessAccount.profile()
                );


        /*
         * =========================================================
         * 2. REQUEST REQUIRES HUMAN REVIEW
         * =========================================================
         */

        if (boundary.status()
                == BusinessBoundaryService.BoundaryStatus.REQUIRES_HUMAN) {

            BusinessRequest reviewRequest =
                    businessRequestService.createForHumanReview(
                            businessAccount.businessId(),
                            customerId,
                            inquiry.service(),
                            inquiry.fields()
                    );


            conversationRepository.remove(
                    customerId
            );

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    "Your request has been received and "
                            + "requires confirmation from a member "
                            + "of the business. We will contact you "
                            + "as soon as possible."
            );
        }


        /*
         * =========================================================
         * 3. REQUEST IS NOT SUPPORTED
         * =========================================================
         */

        if (boundary.status()
                == BusinessBoundaryService.BoundaryStatus.NOT_SUPPORTED) {

            conversationRepository.remove(
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
         * 4. CHECK CURRENT AVAILABILITY INFORMATION
         * =========================================================
         */

        AvailabilityResult availability =
                availabilityService.checkAvailability(
                        inquiry.service(),
                        inquiry.fields(),
                        businessAccount.profile()
                );


        /*
         * =========================================================
         * 5. CREATE BUSINESS REQUEST
         * =========================================================
         */

        BusinessRequest businessRequest =
                businessRequestService.create(
                        businessAccount.businessId(),
                        customerId,
                        inquiry.service(),
                        inquiry.fields(),
                        availability.status()
                );


        /*
         * =========================================================
         * 6. CONVERSATION IS COMPLETE
         * =========================================================
         */

        conversationRepository.remove(
                customerId
        );


        /*
         * =========================================================
         * 7. CUSTOMER RESPONSE
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

conversationRepository.save(
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

        if (service == null
                || service.isBlank()) {

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

        if (definition != null
                && definition.slotPrompts()
                .containsKey(field)) {

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
