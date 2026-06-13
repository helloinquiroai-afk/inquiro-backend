package com.inquiro.inquiry;

import com.inquiro.ai.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryOrchestrator {

    private final AiService aiService;
    private final InquiryProcessor inquiryProcessor;

    public InquiryResponse process(String message) {

        InquiryResult inquiry =
                aiService.analyze(message);

        List<String> missing =
                inquiryProcessor.findMissingFields(inquiry);

        if ("UNKNOWN".equalsIgnoreCase(inquiry.service())) {

            return new InquiryResponse(
                    inquiry,
                    List.of("service"),
                    InquiryStatus.NEEDS_INFORMATION,
                    "Could you tell us what type of service you need?"
            );
        }

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