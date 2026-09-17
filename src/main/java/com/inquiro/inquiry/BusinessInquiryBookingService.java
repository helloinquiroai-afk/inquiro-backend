package com.inquiro.inquiry;

import com.inquiro.ai.AiService;
import com.inquiro.ai.RequestAnalysis;
import com.inquiro.ai.RequestAnalysisValidator;
import com.inquiro.booking.BookingCreationService;
import com.inquiro.booking.BookingEntity;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessBoundaryService;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.SlotFillingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusinessInquiryBookingService {

    private final AiService aiService;
    private final RequestAnalysisValidator validator;
    private final SlotFillingEngine slotFillingEngine;
    private final BusinessBoundaryService businessBoundaryService;
    private final BusinessAccountRepository businessAccountRepository;
    private final BookingCreationService bookingCreationService;

    public Result process(
            String businessId,
            String message,
            String customerName,
            String customerPhone) {

        if (businessId == null || businessId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business ID is required");
        }
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
        }

        BusinessAccount account = businessAccountRepository.findByBusinessId(businessId);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }

        BusinessProfile profile = account.profile();
        RequestAnalysis analysis = aiService.analyzeRequest(message, profile);

        if (validator.needsClarification(analysis)) {
            return Result.notReady("I could not determine what you would like to book. Could you please clarify your request?", List.of());
        }

        BusinessBoundaryService.BoundaryResult boundary =
                businessBoundaryService.check(analysis.intent(), profile);
        if (boundary.status() != BusinessBoundaryService.BoundaryStatus.SUPPORTED) {
            return Result.notReady(boundary.message(), List.of());
        }

        List<String> missing = slotFillingEngine.findMissingSlots(analysis, profile);
        if (!missing.isEmpty()) {
            return Result.notReady(buildMissingReply(missing, profile, analysis.intent()), missing);
        }

        BookingEntity booking = bookingCreationService.create(
                businessId,
                analysis.intent(),
                analysis.entities(),
                customerName,
                customerPhone
        );

        return Result.created(booking, "Your booking has been confirmed.");
    }

    private String buildMissingReply(
            List<String> missing,
            BusinessProfile profile,
            String service) {
        String field = missing.get(0);
        return profile.services().stream()
                .filter(definition -> definition.requestType().equalsIgnoreCase(service))
                .filter(definition -> definition.slotPrompts().containsKey(field))
                .map(definition -> definition.slotPrompts().get(field))
                .findFirst()
                .orElse("Could you please provide " + field.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase() + "?");
    }

    public record Result(
            boolean created,
            String reply,
            List<String> missingFields,
            BookingEntity booking) {

        static Result notReady(String reply, List<String> missingFields) {
            return new Result(false, reply, missingFields, null);
        }

        static Result created(BookingEntity booking, String reply) {
            return new Result(true, reply, List.of(), booking);
        }
    }
}
