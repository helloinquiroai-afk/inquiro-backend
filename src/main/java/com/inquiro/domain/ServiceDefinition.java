package com.inquiro.domain;

import java.util.List;

public record ServiceDefinition(
        String serviceCode,
        List<String> requiredFields
) {
}

