package com.inquiro.request;

import com.inquiro.availability.AvailabilityStatus;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessRequestService;
import com.inquiro.conversation.ConversationRepository;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;
import com.inquiro.inquiry.InquiryStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BusinessRequestActionHandler implements RequestActionHandler {

    private final BusinessRequestService businessRequestService;
    private final ConversationRepository conversationRepository;

    public BusinessRequestActionHandler(
            BusinessRequestService businessRequestService,
            ConversationRepository conversationRepository) {
        this.businessRequestService = businessRequestService;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public RequestActionType actionType() {
        return RequestActionType.BUSINESS_REQUEST;
    }

    @Override
    public InquiryResponse handle(
            BusinessAccount businessAccount,
            String sessionId,
            InquiryResult inquiry) {

        businessRequestService.create(
                businessAccount.businessId(),
                sessionId,
                inquiry.service(),
                inquiry.fields(),
                AvailabilityStatus.UNKNOWN
        );
        conversationRepository.remove(sessionId);

        return new InquiryResponse(
                inquiry,
                List.of(),
                InquiryStatus.INFORMATION_COLLECTED,
                "Your request has been received."
        );
    }
}
