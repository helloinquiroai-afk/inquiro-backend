package com.inquiro.request;

import java.util.List;
import java.util.Map;

public record RequestDefinition(
        String requestType,
        String description,
        List<String> requiredSlots,
        Map<String, String> slotPrompts
) {

    public RequestDefinition(
            String requestType,
            String description,
            List<String> requiredSlots) {

        this(
                requestType,
                description,
                requiredSlots,
                Map.of()
        );
    }

    public RequestDefinition {

        requiredSlots =
                requiredSlots == null
                        ? List.of()
                        : List.copyOf(requiredSlots);

        slotPrompts =
                slotPrompts == null
                        ? Map.of()
                        : Map.copyOf(slotPrompts);
    }
}
