package com.inquiro.pilot;

import com.inquiro.request.RequestActionHandlerRegistry;
import com.inquiro.request.RequestActionType;
import com.inquiro.request.RequestDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenericPilotConfigurationTest {

    @Test
    void bookingActionHasNoImplicitCustomerOrTimeFields() {
        RequestDefinition definition = new RequestDefinition(
                "APPOINTMENT",
                "Appointment",
                List.of("date"),
                Map.of(),
                RequestActionType.BOOKING);

        assertTrue(definition.actionRequiredSlots().isEmpty());
    }

    @Test
    void businessRequestNeverAcquiresBookingFields() {
        RequestDefinition definition = new RequestDefinition(
                "PARTS_SALES",
                "Parts sales",
                List.of("partNumber"),
                Map.of(),
                RequestActionType.BUSINESS_REQUEST);

        assertTrue(definition.actionRequiredSlots().isEmpty());
    }

    @Test
    void explicitActionFieldsRemainBusinessConfigured() {
        RequestDefinition definition = new RequestDefinition(
                "DOCTOR_APPOINTMENT",
                "Doctor appointment",
                List.of("specialty", "date"),
                Map.of(
                        "customerName", "What is your name?",
                        "customerPhone", "What is your phone number?"),
                RequestActionType.BOOKING,
                List.of("customerName", "customerPhone"),
                "TIME_SLOT");

        assertEquals(List.of("customerName", "customerPhone"), definition.actionRequiredSlots());
        assertEquals("TIME_SLOT", definition.availabilityStrategy());

        RequestActionHandlerRegistry registry = new RequestActionHandlerRegistry(List.of());
        assertEquals(List.of("customerName", "customerPhone"), registry.actionRequiredFields(definition));
    }
}
