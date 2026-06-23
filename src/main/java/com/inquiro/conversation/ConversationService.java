package com.inquiro.conversation;

import com.inquiro.ai.AiService;
import com.inquiro.ai.FollowUpAnalysis;
import com.inquiro.ai.RequestAnalysis;
import com.inquiro.inquiry.*;
import com.inquiro.request.SlotFillingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationStore conversationStore;
    private final InquiryOrchestrator inquiryOrchestrator;
    private final AiService aiService;
    private final InquiryProcessor inquiryProcessor;
    private final SlotFillingEngine slotFillingEngine;

    public InquiryResponse process(
            String sessionId,
            String message) {

        ConversationSession session =
                conversationStore.get(sessionId);

        if (session == null) {

            InquiryResponse response =
                    inquiryOrchestrator.process(message);

            if (response.status()
                    == InquiryStatus.NEEDS_INFORMATION) {

                conversationStore.save(
                        new ConversationSession(
                                sessionId,
                                response.inquiry(),
                                response.missingFields(),
                                Instant.now()
                        )
                );
            }

            return response;
        }

        // Phase 2.2:
        // Merge customer reply with existing inquiry

        /*throw new UnsupportedOperationException(
                "Continue conversation not implemented yet");*/

        FollowUpAnalysis replyAnalysis =
                aiService.analyzeFollowUp(
                        session.getInquiry().service(),
                        session.getInquiry().fields(),
                        session.getMissingFields(),
                        message
                );

        Map<String, Object> fields =
                EntityMerger.merge(
                        session.getInquiry().fields(),
                        replyAnalysis.entities()
                );

        RequestAnalysis updatedAnalysis =
                new RequestAnalysis(
                        session.getInquiry().domain(),
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

        List<String> missing =
                slotFillingEngine.findMissingSlots(
                        updatedAnalysis
                );

        if (missing.isEmpty()) {

            conversationStore.remove(sessionId);

            return new InquiryResponse(
                    updatedInquiry,
                    missing,
                    InquiryStatus.READY,
                    "Your request is ready."
            );
        }

        ConversationSession updatedSession =
                new ConversationSession(
                        sessionId,
                        updatedInquiry,
                        missing,
                        Instant.now()
                );

        conversationStore.save(updatedSession);

        return new InquiryResponse(
                updatedInquiry,
                missing,
                InquiryStatus.NEEDS_INFORMATION,
                buildReply(missing)
        );
    }

    private String buildReply(List<String> missingFields) {

        String field = missingFields.get(0);

        return switch (field) {

            case "time" ->
                    "What time would you like the reservation?";

            case "durationNights" ->
                    "How many nights would you like to stay?";

            case "guestCount" ->
                    "How many guests will there be?";

            case "checkInDate" ->
                    "What is your check-in date?";

            case "date" ->
                    "Which date would you prefer?";

            case "specialty" ->
                    "Which specialty do you need?";

            default ->
                    "Could you please provide the missing information?";
        };
    }
}
