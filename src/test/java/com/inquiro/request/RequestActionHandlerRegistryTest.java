package com.inquiro.request;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestActionHandlerRegistryTest {

    @Test
    void selectsHandlerByConfiguredActionAndUsesConfiguredCompletionSlots() {
        RequestActionHandler booking = new StubHandler(RequestActionType.BOOKING);
        RequestActionHandler request = new StubHandler(RequestActionType.BUSINESS_REQUEST);
        RequestActionHandler review = new StubHandler(RequestActionType.HUMAN_REVIEW);

        RequestActionHandlerRegistry registry =
                new RequestActionHandlerRegistry(List.of(booking, request, review));

        RequestDefinition definition = new RequestDefinition(
                "DOCTOR_APPOINTMENT",
                "Doctor appointment",
                List.of("specialty", "date", "time"),
                Map.of(),
                RequestActionType.BOOKING,
                List.of("customerName", "customerPhone"),
                "TIME_SLOT"
        );

        assertEquals(booking, registry.handlerFor(definition));
        assertEquals(
                List.of("customerName", "customerPhone"),
                registry.actionRequiredFields(definition)
        );
    }

    @Test
    void nonBookingActionsDoNotAcquireBookingSpecificFields() {
        RequestActionHandlerRegistry registry =
                new RequestActionHandlerRegistry(List.of(
                        new StubHandler(RequestActionType.BUSINESS_REQUEST),
                        new StubHandler(RequestActionType.HUMAN_REVIEW)
                ));

        RequestDefinition definition = new RequestDefinition(
                "CAR_SERVICE",
                "Vehicle service request",
                List.of("vehicleNumber", "serviceType"),
                Map.of(),
                RequestActionType.BUSINESS_REQUEST
        );

        assertEquals(List.of(), registry.actionRequiredFields(definition));
    }

    private static final class StubHandler implements RequestActionHandler {
        private final RequestActionType type;

        private StubHandler(RequestActionType type) {
            this.type = type;
        }

        @Override
        public RequestActionType actionType() {
            return type;
        }

        @Override
        public com.inquiro.inquiry.InquiryResponse handle(
                com.inquiro.business.BusinessAccount businessAccount,
                String sessionId,
                com.inquiro.inquiry.InquiryResult inquiry) {
            throw new UnsupportedOperationException();
        }
    }
}
