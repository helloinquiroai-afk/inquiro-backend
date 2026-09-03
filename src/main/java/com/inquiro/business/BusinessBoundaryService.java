package com.inquiro.business;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class BusinessBoundaryService {

    public BoundaryResult check(
            String service,
            BusinessProfile businessProfile) {

        if (businessProfile == null
                || businessProfile.knowledge() == null
                || businessProfile.knowledge().boundaries() == null) {

            return new BoundaryResult(
                    BoundaryStatus.REQUIRES_HUMAN,
                    "I’m unable to determine whether this request is "
                            + "supported by the business. "
                            + "A member of the business will need to assist you."
            );
        }

        BusinessBoundaries boundaries =
                businessProfile.knowledge().boundaries();

        String normalizedService =
                normalize(service);

        /*
         * 1. Explicitly not supported
         */
        if (contains(
                boundaries.notSupported(),
                normalizedService)) {

            return new BoundaryResult(
                    BoundaryStatus.NOT_SUPPORTED,
                    "Sorry, this business does not provide "
                            + "the requested service."
            );
        }

        /*
         * 2. Explicitly requires human handling
         */
        if (contains(
                boundaries.requiresHuman(),
                normalizedService)) {

            return new BoundaryResult(
                    BoundaryStatus.REQUIRES_HUMAN,
                    "This request requires confirmation "
                            + "from a member of the business."
            );
        }

        /*
         * 3. Explicitly supported
         */
        if (contains(
                boundaries.supported(),
                normalizedService)) {

            return new BoundaryResult(
                    BoundaryStatus.SUPPORTED,
                    null
            );
        }

        /*
         * 4. Unknown service
         *
         * Do NOT automatically assume that an unknown service
         * is supported.
         */
        return new BoundaryResult(
                BoundaryStatus.NOT_SUPPORTED,
                "Sorry, I could not confirm that this business "
                        + "provides the requested service."
        );
    }

    private boolean contains(
            java.util.List<String> values,
            String requestedService) {

        if (values == null || requestedService == null) {
            return false;
        }

        return values.stream()
                .filter(value -> value != null)
                .map(this::normalize)
                .anyMatch(requestedService::equals);
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        return value
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");
    }

    public enum BoundaryStatus {

        SUPPORTED,

        NOT_SUPPORTED,

        REQUIRES_HUMAN
    }

    public record BoundaryResult(
            BoundaryStatus status,
            String message
    ) {
    }
}
