package com.inquiro.booking;

import com.inquiro.business.BusinessAccount;
import com.inquiro.conversation.ConversationRepository;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;
import com.inquiro.inquiry.InquiryStatus;
import com.inquiro.request.RequestActionHandler;
import com.inquiro.request.RequestActionType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingRequestActionHandler implements RequestActionHandler {
    private final BookingCreationService bookingCreationService;
    private final ConversationRepository conversationRepository;

    public BookingRequestActionHandler(
            BookingCreationService bookingCreationService,
            ConversationRepository conversationRepository) {
        this.bookingCreationService = bookingCreationService;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public RequestActionType actionType() {
        return RequestActionType.BOOKING;
    }

    @Override
    public InquiryResponse handle(
            BusinessAccount businessAccount,
            String sessionId,
            InquiryResult inquiry) {
        String customerName = value(inquiry, "customerName");
        String customerPhone = value(inquiry, "customerPhone");
        if (customerName == null || customerPhone == null) {
            return new InquiryResponse(
                    inquiry,
                    List.of("customerName", "customerPhone"),
                    InquiryStatus.NEEDS_INFORMATION,
                    "Please provide your name and phone number."
            );
        }

        BookingEntity booking = bookingCreationService.create(
                businessAccount.businessId(),
                inquiry.service(),
                inquiry.fields(),
                customerName,
                customerPhone,
                sessionId
        );
        conversationRepository.remove(sessionId);

        return new InquiryResponse(
                inquiry,
                List.of(),
                InquiryStatus.INFORMATION_COLLECTED,
                "Your booking is confirmed. Booking reference: " + booking.getBookingId(),
                "",
                booking.getBookingId()
        );
    }

    private String value(InquiryResult inquiry, String key) {
        Object value = inquiry.fields() == null ? null : inquiry.fields().get(key);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
