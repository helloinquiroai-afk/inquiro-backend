package com.inquiro.request;

import java.util.List;
import java.util.Map;

public record RequestDefinition(
        String requestType,
        String description,
        List<String> requiredSlots,
        Map<String, String> slotPrompts,
        RequestActionType actionType,
        List<String> actionRequiredSlots,
        String availabilityStrategy
) {

    public RequestDefinition(
            String requestType,
            String description,
            List<String> requiredSlots) {
        this(requestType, description, requiredSlots, Map.of(), RequestActionType.BUSINESS_REQUEST);
    }

    public RequestDefinition(
            String requestType,
            String description,
            List<String> requiredSlots,
            Map<String, String> slotPrompts) {
        this(requestType, description, requiredSlots, slotPrompts, RequestActionType.BUSINESS_REQUEST);
    }

    public RequestDefinition(
            String requestType,
            String description,
            List<String> requiredSlots,
            Map<String, String> slotPrompts,
            RequestActionType actionType) {
        this(
                requestType,
                description,
                requiredSlots,
                slotPrompts,
                actionType,
                null,
                null
        );
    }

    public RequestDefinition {
        requiredSlots = requiredSlots == null ? List.of() : List.copyOf(requiredSlots);
        slotPrompts = slotPrompts == null ? Map.of() : Map.copyOf(slotPrompts);
        actionType = actionType == null ? RequestActionType.BOOKING : actionType;

        if (actionRequiredSlots == null) {
            actionRequiredSlots = actionType == RequestActionType.BOOKING
                    ? List.of("time", "customerName", "customerPhone")
                    : List.of();
        } else {
            actionRequiredSlots = List.copyOf(actionRequiredSlots);
        }

        availabilityStrategy = normalizeAvailabilityStrategy(
                availabilityStrategy,
                actionType,
                requiredSlots
        );
    }

    private static String normalizeAvailabilityStrategy(
            String configured,
            RequestActionType actionType,
            List<String> requiredSlots) {

        if (configured != null && !configured.isBlank()) {
            return configured.trim().toUpperCase();
        }

        if (actionType != RequestActionType.BOOKING) {
            return "NONE";
        }

        boolean dateRange = requiredSlots.stream()
                .anyMatch(slot -> "checkInDate".equalsIgnoreCase(slot)
                        || "checkOutDate".equalsIgnoreCase(slot)
                        || "durationNights".equalsIgnoreCase(slot));

        return dateRange ? "DATE_RANGE" : "TIME_SLOT";
    }
}
