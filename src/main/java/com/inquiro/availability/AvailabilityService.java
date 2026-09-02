package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final List<AvailabilitySource> sources;

    public AvailabilityResult checkAvailability(
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile) {

        if (sources == null || sources.isEmpty()) {
            return unknown();
        }

        AvailabilityResult bestKnown =
                null;

        AvailabilityResult bestUnknown =
                null;

        for (AvailabilitySource source : sources) {

            AvailabilityResult result =
                    source.check(
                            service,
                            fields,
                            businessProfile
                    );

            if (result == null) {
                continue;
            }

            if (result.status() == AvailabilityStatus.UNKNOWN) {

                if (bestUnknown == null) {
                    bestUnknown = result;
                }

                continue;
            }

            if (bestKnown == null ||
                    priority(result.status())
                            < priority(bestKnown.status())) {

                bestKnown = result;
            }
        }

        if (bestKnown != null) {
            return bestKnown;
        }

        if (bestUnknown != null) {
            return bestUnknown;
        }

        return unknown();
    }

    private int priority(
            AvailabilityStatus status) {

        return switch (status) {
            case CONFIRMED -> 0;
            case UNAVAILABLE -> 1;
            case INDICATED -> 2;
            case UNKNOWN -> 3;
        };
    }

    private AvailabilityResult unknown() {

        return new AvailabilityResult(
                AvailabilityStatus.UNKNOWN,
                "We have received your request. The business will confirm availability shortly."
        );
    }
}
