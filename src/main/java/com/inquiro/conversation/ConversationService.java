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
import com.inquiro.business.onboarding.OnboardingService;
import com.inquiro.business.onboarding.OnboardingSummary;
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
    private final OnboardingService onboardingService;

    public InquiryResponse process(String sessionId, String externalChannelId) {
        return process(sessionId, BusinessChannelType.MESSENGER, externalChannelId, null);
    }

    public InquiryResponse process(String sessionId, String externalChannelId, String message) {
        return process(sessionId, BusinessChannelType.MESSENGER, externalChannelId, message);
    }

    public InquiryResponse process(String sessionId, BusinessChannelType channelType, String externalChannelId, String message) {
        BusinessChannel channel = businessChannelRepository.findByTypeAndExternalId(channelType, externalChannelId);
        if (channel == null) throw new IllegalStateException("No business channel configured for external ID: " + externalChannelId);
        if (!channel.enabled()) throw new IllegalStateException("Business channel is disabled: " + externalChannelId);

        BusinessAccount businessAccount = businessAccountRepository.findByBusinessId(channel.businessId());
        if (businessAccount == null) throw new IllegalStateException("No business configured for business ID: " + channel.businessId());

        OnboardingSummary onboarding = onboardingService.getOnboardingSummary(businessAccount.businessId());
        if (onboarding == null || !onboarding.readyForReceptionist()) throw new IllegalStateException("Business is not ready for receptionist");

        BusinessProfile profile = businessAccount.profile();
        String canonicalSessionId = new ConversationIdentity(businessAccount.businessId(), channelType, externalChannelId, sessionId).sessionId();
        ConversationSession session = conversationRepository.find(canonicalSessionId);

        if (session == null) {
            InquiryResponse response = inquiryOrchestrator.process(message, profile);
            if (response.status() == InquiryStatus.NEEDS_INFORMATION) {
                saveConversation(canonicalSessionId, response);
                return response;
            }
            if (response.status() == InquiryStatus.INFORMATION_COLLECTED && isBusinessRequest(response.inquiry())) {
                return processCompletedRequest(businessAccount, canonicalSessionId, response.inquiry()).withKnowledgeReply(response.knowledgeReply());
            }
            return response;
        }

        ConversationIntentAnalysis intent = aiService.analyzeConversationIntent(profile, session.getInquiry().service(), session.getInquiry().fields(), session.getMissingFields(), message);
        if ("BUSINESS_QUESTION".equalsIgnoreCase(intent.intent())) {
            String answer = inquiryOrchestrator.answerKnowledgeQuestions(intent.knowledgeQuestions().isEmpty() ? List.of(message) : intent.knowledgeQuestions(), profile);
            return new InquiryResponse(session.getInquiry(), session.getMissingFields(), InquiryStatus.NEEDS_INFORMATION, answer);
        }

        if ("NEW_REQUEST".equalsIgnoreCase(intent.intent())) {
            InquiryResponse response = inquiryOrchestrator.process(message, profile);
            if (!isBusinessRequest(response.inquiry())) return response;
            conversationRepository.remove(canonicalSessionId);
            if (response.status() == InquiryStatus.NEEDS_INFORMATION) {
                saveConversation(canonicalSessionId, response);
                return response;
            }
            if (response.status() == InquiryStatus.INFORMATION_COLLECTED) {
                return processCompletedRequest(businessAccount, canonicalSessionId, response.inquiry()).withKnowledgeReply(response.knowledgeReply());
            }
            return response;
        }

        String knowledgeReply = inquiryOrchestrator.answerKnowledgeQuestions(intent.knowledgeQuestions(), profile);
        FollowUpAnalysis replyAnalysis = aiService.analyzeFollowUp(session.getInquiry().service(), session.getInquiry().fields(), session.getMissingFields(), message);
        Map<String, Object> fields = EntityMerger.merge(session.getInquiry().fields(), replyAnalysis.entities());
        RequestAnalysis updatedAnalysis = new RequestAnalysis(session.getInquiry().service(), 1.0, fields);
        InquiryResult updatedInquiry = new InquiryResult(session.getInquiry().domain(), session.getInquiry().service(), fields);
        List<String> missing = slotFillingEngine.findMissingSlots(updatedAnalysis, profile);

        if (!missing.isEmpty()) {
            conversationRepository.save(new ConversationSession(canonicalSessionId, updatedInquiry, missing, Instant.now()));
            return new InquiryResponse(updatedInquiry, missing, InquiryStatus.NEEDS_INFORMATION, buildReply(missing, profile, updatedInquiry.service())).withKnowledgeReply(knowledgeReply);
        }
        return processCompletedRequest(businessAccount, canonicalSessionId, updatedInquiry).withKnowledgeReply(knowledgeReply);
    }

    private InquiryResponse processCompletedRequest(BusinessAccount businessAccount, String sessionId, InquiryResult inquiry) {
        if (!isBusinessRequest(inquiry)) return new InquiryResponse(inquiry, List.of(), InquiryStatus.INFORMATION_COLLECTED, "Thank you.");
        BusinessBoundaryService.BoundaryResult boundary = businessBoundaryService.check(inquiry.service(), businessAccount.profile());
        if (boundary.status() == BusinessBoundaryService.BoundaryStatus.REQUIRES_HUMAN) {
            businessRequestService.createForHumanReview(businessAccount.businessId(), sessionId, inquiry.service(), inquiry.fields());
            conversationRepository.remove(sessionId);
            return new InquiryResponse(inquiry, List.of(), InquiryStatus.INFORMATION_COLLECTED, "Your request has been received and requires confirmation from a member of the business. We will contact you as soon as possible.");
        }
        if (boundary.status() == BusinessBoundaryService.BoundaryStatus.NOT_SUPPORTED) {
            conversationRepository.remove(sessionId);
            return new InquiryResponse(inquiry, List.of(), InquiryStatus.INFORMATION_COLLECTED, boundary.message());
        }
        AvailabilityResult availability = availabilityService.checkAvailability(inquiry.service(), inquiry.fields(), businessAccount.profile());
        businessRequestService.create(businessAccount.businessId(), sessionId, inquiry.service(), inquiry.fields(), availability.status());
        conversationRepository.remove(sessionId);
        return buildAvailabilityResponse(inquiry, availability);
    }

    private void saveConversation(String sessionId, InquiryResponse response) {
        conversationRepository.save(new ConversationSession(sessionId, response.inquiry(), response.missingFields(), Instant.now()));
    }

    private boolean isBusinessRequest(InquiryResult inquiry) {
        if (inquiry == null || inquiry.service() == null || inquiry.service().isBlank()) return false;
        String service = inquiry.service();
        return !"UNKNOWN".equalsIgnoreCase(service) && !"GREETING".equalsIgnoreCase(service) && !"BUSINESS_QUESTION".equalsIgnoreCase(service);
    }

    private InquiryResponse buildAvailabilityResponse(InquiryResult inquiry, AvailabilityResult availability) {
        String reply = availability == null || availability.message() == null ? "Thank you. The business will confirm availability and contact you as soon as possible." : availability.message();
        return new InquiryResponse(inquiry, List.of(), InquiryStatus.INFORMATION_COLLECTED, reply);
    }

    private String buildReply(List<String> missingFields, BusinessProfile profile, String service) {
        String field = missingFields.get(0);
        RequestDefinition definition = profile.services().stream().filter(candidate -> candidate.requestType().equalsIgnoreCase(service)).findFirst().orElse(null);
        if (definition != null && definition.slotPrompts().containsKey(field)) return definition.slotPrompts().get(field);
        return "Could you please provide " + field.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase() + "?";
    }
}
