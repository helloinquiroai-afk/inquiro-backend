package com.inquiro.inquiry;

import com.inquiro.ai.AiService;
import com.inquiro.ai.RequestAnalysis;
import com.inquiro.ai.RequestAnalysisValidator;
import com.inquiro.request.SlotFillingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InquiryOrchestrator {

    private final AiService aiService;
    private final InquiryProcessor inquiryProcessor;
    private final SlotFillingEngine slotFillingEngine;
    private final RequestAnalysisValidator validator;

    public InquiryResponse process(String message) {

        RequestAnalysis analysis =
                aiService.analyzeRequest(message);

        if (validator.needsClarification(analysis)) {

            return new InquiryResponse(
                    new InquiryResult(
                            "UNKNOWN",
                            "UNKNOWN",
                            Map.of()
                    ),
                    List.of(),
                    InquiryStatus.NEEDS_CLARIFICATION,
                    "Could you tell me what type of service you need? For example: accommodation, restaurant reservation, airport pickup, or doctor appointment."
            );
        }

        List<String> missing =
                slotFillingEngine.findMissingSlots(
                        analysis
                );

        InquiryResult inquiry =
                new InquiryResult(
                        analysis.domain(),
                        analysis.requestType(),
                        analysis.entities()
                );

        if (missing.isEmpty()) {

            return new InquiryResponse(
                    inquiry,
                    missing,
                    InquiryStatus.READY,
                    "Your request is ready."
            );
        }

        return new InquiryResponse(
                inquiry,
                missing,
                InquiryStatus.NEEDS_INFORMATION,
                buildReply(missing)
        );
    }

    private String buildReply(List<String> missingFields) {

        String field = missingFields.get(0);

        return switch (field) {

            case "location" ->
                    "Where would you like to stay?";

            case "pickupLocation" ->
                    "Where would you like to be picked up from?";

            case "passengerCount" ->
                    "How many passengers will be travelling?";

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