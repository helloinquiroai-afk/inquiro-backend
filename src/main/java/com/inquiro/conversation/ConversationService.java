package com.inquiro.conversation;

import com.inquiro.ai.AiService;
import com.inquiro.ai.ConversationIntentAnalysis;
import com.inquiro.ai.FollowUpAnalysis;
import com.inquiro.ai.RequestAnalysis;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessBoundaryService;
import com.inquiro.business.BusinessChannel;
import com.inquiro.business.BusinessChannelRepository;
import com.inquiro.business.BusinessChannelType;
import com.inquiro.business.BusinessProfile;
import com.inquiro.business.BusinessRequestService;
import com.inquiro.business.onboarding.OnboardingService;
import com.inquiro.business.onboarding.OnboardingSummary;
import com.inquiro.booking.BookingCreationService;
import com.inquiro.booking.BookingEntity;
import com.inquiro.inquiry.InquiryOrchestrator;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;
import com.inquiro.inquiry.InquiryStatus;
import com.inquiro.request.RequestDefinition;
import com.inquiro.request.SlotFillingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ConversationService {

    @Autowired
    public ConversationService(
            ConversationRepository conversationRepository,
            InquiryOrchestrator inquiryOrchestrator,
            AiService aiService,
            SlotFillingEngine slotFillingEngine,
            BusinessAccountRepository businessAccountRepository,
            BusinessChannelRepository businessChannelRepository,
            BusinessRequestService businessRequestService,
            BusinessBoundaryService businessBoundaryService,
            OnboardingService onboardingService,
            BookingCreationService bookingCreationService) {
        this.conversationRepository = conversationRepository;
        this.inquiryOrchestrator = inquiryOrchestrator;
        this.aiService = aiService;
        this.slotFillingEngine = slotFillingEngine;
        this.businessAccountRepository = businessAccountRepository;
        this.businessChannelRepository = businessChannelRepository;
        this.businessRequestService = businessRequestService;
        this.businessBoundaryService = businessBoundaryService;
        this.onboardingService = onboardingService;
        this.bookingCreationService = bookingCreationService;
    }

    /** Compatibility constructor for existing unit tests and non-booking callers. */
    public ConversationService(
            ConversationRepository conversationRepository,
            InquiryOrchestrator inquiryOrchestrator,
            AiService aiService,
            SlotFillingEngine slotFillingEngine,
            BusinessAccountRepository businessAccountRepository,
            BusinessChannelRepository businessChannelRepository,
            AvailabilityService ignoredAvailabilityService,
            BusinessRequestService businessRequestService,
            BusinessBoundaryService businessBoundaryService,
            OnboardingService onboardingService) {
        this(conversationRepository, inquiryOrchestrator, aiService, slotFillingEngine,
                businessAccountRepository, businessChannelRepository, businessRequestService,
                businessBoundaryService, onboardingService, null);
    }
    private static final String TIME = "time";
    private static final String CUSTOMER_NAME = "customerName";
    private static final String CUSTOMER_PHONE = "customerPhone";

    private final ConversationRepository conversationRepository;
    private final InquiryOrchestrator inquiryOrchestrator;
    private final AiService aiService;
    private final SlotFillingEngine slotFillingEngine;
    private final BusinessAccountRepository businessAccountRepository;
    private final BusinessChannelRepository businessChannelRepository;
    private final BusinessRequestService businessRequestService;
    private final BusinessBoundaryService businessBoundaryService;
    private final OnboardingService onboardingService;
    private final BookingCreationService bookingCreationService;

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
            if (response.status() == InquiryStatus.NEEDS_INFORMATION) return saveConversation(canonicalSessionId, response, profile);
            if (response.status() == InquiryStatus.INFORMATION_COLLECTED && isBusinessRequest(response.inquiry()))
                return processCompletedRequest(businessAccount, canonicalSessionId, response.inquiry()).withKnowledgeReply(response.knowledgeReply());
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
            if (response.status() == InquiryStatus.NEEDS_INFORMATION) return saveConversation(canonicalSessionId, response, profile);
            if (response.status() == InquiryStatus.INFORMATION_COLLECTED)
                return processCompletedRequest(businessAccount, canonicalSessionId, response.inquiry()).withKnowledgeReply(response.knowledgeReply());
            return response;
        }

        String knowledgeReply = inquiryOrchestrator.answerKnowledgeQuestions(intent.knowledgeQuestions(), profile);
        FollowUpAnalysis replyAnalysis = aiService.analyzeFollowUp(session.getInquiry().service(), session.getInquiry().fields(), session.getMissingFields(), message);
        Map<String, Object> fields = EntityMerger.merge(session.getInquiry().fields(), replyAnalysis.entities());
        RequestAnalysis updatedAnalysis = new RequestAnalysis(session.getInquiry().service(), 1.0, fields);
        InquiryResult updatedInquiry = new InquiryResult(session.getInquiry().domain(), session.getInquiry().service(), fields);
        List<String> workflowMissing = slotFillingEngine.findMissingSlots(updatedAnalysis, profile);
        List<String> missing = bookingMissingFields(updatedInquiry, workflowMissing);

        if (!missing.isEmpty()) {
            ConversationSession updatedSession = new ConversationSession(canonicalSessionId, updatedInquiry, missing, Instant.now());
            conversationRepository.save(updatedSession);
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

        String customerName = value(inquiry.fields(), CUSTOMER_NAME);
        String customerPhone = value(inquiry.fields(), CUSTOMER_PHONE);
        try {
            BookingEntity booking = bookingCreationService.create(businessAccount.businessId(), inquiry.service(), inquiry.fields(), customerName, customerPhone);
            conversationRepository.remove(sessionId);
            return new InquiryResponse(inquiry, List.of(), InquiryStatus.INFORMATION_COLLECTED,
                    "Your booking has been confirmed. Booking ID: " + booking.getBookingId() + ".", booking.getBookingId());
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            conversationRepository.remove(sessionId);
            throw exception;
        }
    }

    private InquiryResponse saveConversation(String sessionId, InquiryResponse response, BusinessProfile profile) {
        List<String> missing = bookingMissingFields(response.inquiry(), response.missingFields());
        InquiryResponse updated = new InquiryResponse(response.inquiry(), missing, response.status(), response.reply(), response.knowledgeReply(), response.bookingId());
        conversationRepository.save(new ConversationSession(sessionId, updated.inquiry(), updated.missingFields(), Instant.now()));
        if (missing.equals(response.missingFields())) return response;
        return new InquiryResponse(updated.inquiry(), missing, updated.status(), buildReply(missing, profile, updated.inquiry().service()), updated.knowledgeReply(), updated.bookingId());
    }

    private List<String> bookingMissingFields(InquiryResult inquiry, List<String> workflowMissing) {
        List<String> missing = new ArrayList<>(workflowMissing == null ? List.of() : workflowMissing);
        if (isBookableService(inquiry)) {
            if (!containsField(inquiry, TIME)) missing.add(TIME);
            if (!containsField(inquiry, CUSTOMER_NAME)) missing.add(CUSTOMER_NAME);
            if (!containsField(inquiry, CUSTOMER_PHONE)) missing.add(CUSTOMER_PHONE);
        }
        return List.copyOf(missing);
    }

    private boolean containsField(InquiryResult inquiry, String field) {
        return inquiry != null && inquiry.fields() != null && inquiry.fields().get(field) != null && !String.valueOf(inquiry.fields().get(field)).isBlank();
    }

    private boolean isBookableService(InquiryResult inquiry) {
        return isBusinessRequest(inquiry);
    }

    private String value(Map<String, Object> fields, String key) {
        if (fields == null) return null;
        Object value = fields.get(key);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private boolean isBusinessRequest(InquiryResult inquiry) {
        if (inquiry == null || inquiry.service() == null || inquiry.service().isBlank()) return false;
        String service = inquiry.service();
        return !"UNKNOWN".equalsIgnoreCase(service) && !"GREETING".equalsIgnoreCase(service) && !"BUSINESS_QUESTION".equalsIgnoreCase(service);
    }

    private String buildReply(List<String> missingFields, BusinessProfile profile, String service) {
        String field = missingFields.get(0);
        RequestDefinition definition = profile.services().stream().filter(candidate -> candidate.requestType().equalsIgnoreCase(service)).findFirst().orElse(null);
        if (definition != null && definition.slotPrompts().containsKey(field)) return definition.slotPrompts().get(field);
        return switch (field) {
            case TIME -> "What time would you like to book?";
            case CUSTOMER_NAME -> "May I have your name?";
            case CUSTOMER_PHONE -> "What phone number should we use for the booking?";
            default -> "Could you please provide " + field.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase() + "?";
        };
    }
}
