package com.inquiro.request;

import com.inquiro.business.BusinessAccount;
import com.inquiro.booking.BookingCreationService;
import com.inquiro.booking.BookingEntity;
import com.inquiro.conversation.ConversationRepository;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;
import com.inquiro.inquiry.InquiryStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class BookingActionHandler implements RequestActionHandler {

    private final BookingCreationService bookingCreationService;
    private final ConversationRepository conversationRepository;

    public BookingActionHandler(
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

        try {
            BookingEntity booking = bookingCreationService.create(
                    businessAccount.businessId(),
                    inquiry.service(),
                    inquiry.fields(),
                    customerName,
                    customerPhone
            );

            conversationRepository.remove(sessionId);

            return new InquiryResponse(
                    inquiry,
                    List.of(),
                    InquiryStatus.INFORMATION_COLLECTED,
                    "Your booking has been confirmed. Booking ID: " + booking.getBookingId() + ".",
                    booking.getBookingId()
            );
        } catch (ResponseStatusException exception) {
            conversationRepository.remove(sessionId);
            throw exception;
        }


    }

    private String value(InquiryResult inquiry, String key) {
        if (inquiry == null || inquiry.fields() == null) {
            return null;
        }
        Object value = inquiry.fields().get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
