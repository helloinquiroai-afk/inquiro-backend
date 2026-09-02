package com.inquiro.availability;

import com.inquiro.business.BusinessBoundaries;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class BusinessKnowledgeAvailabilitySource
        implements AvailabilitySource {

    @Override
    public AvailabilityResult check(
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile) {

        BusinessBoundaries boundaries =
                businessProfile.knowledge()
                        .boundaries();

        if (matchesAny(
                service,
                fields,
                boundaries.notSupported())) {

            return new AvailabilityResult(
                    AvailabilityStatus.UNAVAILABLE,
                    "The business information says this request is not supported."
            );
        }

        if (matchesAny(
                service,
                fields,
                boundaries.requiresHuman())) {

            return new AvailabilityResult(
                    AvailabilityStatus.UNKNOWN,
                    "This request requires review by the business before it can be confirmed."
            );
        }

        RequestDefinition definition =
                businessProfile.services()
                        .stream()
                        .filter(candidate ->
                                candidate.requestType()
                                        .equalsIgnoreCase(service))
                        .findFirst()
                        .orElse(null);

        if (definition != null) {

            return new AvailabilityResult(
                    AvailabilityStatus.INDICATED,
                    "Business information indicates that "
                            + definition.description()
                            + " is offered. The business will confirm availability shortly."
            );
        }

        return new AvailabilityResult(
                AvailabilityStatus.UNKNOWN,
                "Availability could not be determined from the current business information."
        );
    }

    private boolean matchesAny(
            String service,
            Map<String, Object> fields,
            Iterable<String> boundaries) {

        for (String boundary : boundaries) {

            if (matches(
                    service,
                    boundary
            )) {
                return true;
            }

            for (Object value : fields.values()) {

                if (matches(
                        String.valueOf(value),
                        boundary
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matches(
            String value,
            String boundary) {

        if (value == null || boundary == null) {
            return false;
        }

        String normalizedValue =
                normalize(value);

        String normalizedBoundary =
                normalize(boundary);

        return normalizedValue.contains(normalizedBoundary)
                || normalizedBoundary.contains(normalizedValue);
    }

    private String normalize(
            String value) {

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }
}
