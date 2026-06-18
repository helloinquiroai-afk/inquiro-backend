package com.inquiro.request;

import java.util.List;

public record RequestDefinition(

        String requestType,

        List<String> requiredSlots

) {
}
