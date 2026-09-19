package com.inquiro.request;

import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessRequestService;
import com.inquiro.conversation.ConversationRepository;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;
import com.inquiro.inquiry.InquiryStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HumanReviewActionHandler implements RequestActionHandler {

    private final BusinessRequestService businessRequestService;
    private final ConversationRepository conversationRepository;

    public HumanReviewActionHandler(
            BusinessRequestService businessRequestService,
            ConversationRepository conversationRepository) {
        this.businessRequestService = businessRequestService;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public RequestActionType actionType() {
        return RequestActionType.HUMAN_REVIEW;
    }

    @Override
    public InquiryResponse handle(
            BusinessAccount businessAccount,
            String sessionId,
            InquiryResult inquiry) {

        businessRequestService.createForHumanReview(
                businessAccount.businessId(),
                sessionId,
                inquiry.service(),
                inquiry.fields()
        );
        conversationRepository.remove(sessionId);

        return new InquiryResponse(
                inquiry,
                List.of(),
                InquiryStatus.INFORMATION_COLLECTED,
                "Your request has been received and will be reviewed by the business."
        );
    }
}
