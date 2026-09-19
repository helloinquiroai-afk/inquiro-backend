package com.inquiro.core;

import com.inquiro.business.BusinessProfile;
import com.inquiro.knowledge.BusinessKnowledgeExtractor;
import com.inquiro.knowledge.KnowledgeDocument;
import com.inquiro.request.RequestActionType;
import com.inquiro.request.RequestDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreEngineConfigurationTest {

    @Test
    void dateRangeBookingDoesNotRequireAppointmentTime() {
        RequestDefinition definition = new RequestDefinition(
                "ROOM_BOOKING",
                "Room booking",
                List.of("checkInDate", "durationNights", "guestCount"),
                Map.of(),
                RequestActionType.BOOKING
        );

        assertEquals(RequestActionType.BOOKING, definition.actionType());
        assertTrue(definition.actionRequiredSlots().isEmpty());
        assertEquals("AUTO", definition.availabilityStrategy());
    }

    @Test
    void timeSlotBookingStillRequiresTimeAndCustomerDetails() {
        RequestDefinition definition = new RequestDefinition(
                "TABLE_RESERVATION",
                "Table reservation",
                List.of("date", "time", "guestCount"),
                Map.of(),
                RequestActionType.BOOKING
        );

        assertTrue(definition.actionRequiredSlots().isEmpty());
    }

    @Test
    void knowledgeExtractorDoesNotTurnEveryServiceIntoBooking() {
        String document = """
                Business Name: ABC Auto Care
                Business Type: AUTOMOTIVE

                Services:
                - CAR_SERVICE
                - PARTS_SALES

                Booking: required

                Service,Available,Requirements
                CAR_SERVICE,yes,vehicleNumber, serviceType
                PARTS_SALES,yes,partNumber
                """;

        BusinessProfile profile = new BusinessKnowledgeExtractor().extract(
                new KnowledgeDocument(
                        com.inquiro.knowledge.KnowledgeSource.TEXT,
                        document,
                        Map.of()
                )
        );

        assertEquals(2, profile.services().size());
        assertEquals(RequestActionType.BUSINESS_REQUEST, profile.services().get(0).actionType());
        assertEquals(RequestActionType.BUSINESS_REQUEST, profile.services().get(1).actionType());
    }

    @Test
    void knowledgeExtractorRecognizesDateRangeBookingShape() {
        String document = """
                Business Name: Paris Grand Hotel
                Business Type: HOSPITALITY

                Services:
                - ROOM_BOOKING

                Service,Available,Requirements
                ROOM_BOOKING,yes,checkInDate, durationNights, guestCount
                """;

        BusinessProfile profile = new BusinessKnowledgeExtractor().extract(
                new KnowledgeDocument(
                        com.inquiro.knowledge.KnowledgeSource.TEXT,
                        document,
                        Map.of()
                )
        );

        RequestDefinition definition = profile.services().get(0);
        assertEquals(RequestActionType.BOOKING, definition.actionType());
        assertEquals("AUTO", definition.availabilityStrategy());
        assertTrue(definition.actionRequiredSlots().isEmpty());
    }
}
