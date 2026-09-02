package com.inquiro.availability;

public record AvailabilityResult(
        AvailabilityStatus status,
        String message
) {
}
