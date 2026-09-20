package com.inquiro.business;

public record BusinessLocation(
        String label,
        String address,
        Double latitude,
        Double longitude
) {
    public BusinessLocation {
        label = label == null ? "" : label.trim();
        address = address == null ? "" : address.trim();
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    public boolean isMeaningful() {
        return !address.isBlank() || hasCoordinates();
    }
}
