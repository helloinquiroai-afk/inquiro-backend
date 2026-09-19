package com.inquiro.request;

import java.util.List;
import java.util.Map;

public record RequestDefinition(
        String requestType,
        String description,
        List<String> requiredSlots,
        Map<String, String> slotPrompts,
        RequestActionType actionType
) {

    public RequestDefinition(
            String requestType,
            String description,
            List<String> requiredSlots) {
        this(requestType, description, requiredSlots, Map.of(), RequestActionType.BOOKING);
    }

    public RequestDefinition(
            String requestType,
            String description,
            List<String> requiredSlots,
            Map<String, String> slotPrompts) {
        this(requestType, description, requiredSlots, slotPrompts, RequestActionType.BOOKING);
    }

    public RequestDefinition {
        requiredSlots = requiredSlots == null ? List.of() : List.copyOf(requiredSlots);
        slotPrompts = slotPrompts == null ? Map.of() : Map.copyOf(slotPrompts);
        actionType = actionType == null ? RequestActionType.BOOKING : actionType;
    }
}
