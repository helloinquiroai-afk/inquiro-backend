package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.booking.BookingJpaRepository;
import com.inquiro.request.RequestDefinition;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Service
public class BookingAvailabilityService {

    private final BookingAvailabilityStrategyRegistry strategyRegistry;

    @Autowired
    public BookingAvailabilityService(
            BookingJpaRepository bookingRepository,
            BusinessScheduleAvailabilitySource scheduleSource,
            BookingAvailabilityStrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
    }

    /**
     * Compatibility constructor retained for existing unit tests.
     */
    public BookingAvailabilityService(
            BookingJpaRepository bookingRepository,
            BusinessScheduleAvailabilitySource scheduleSource) {
        this.strategyRegistry = new BookingAvailabilityStrategyRegistry(
                java.util.List.of(
                        new DateRangeBookingAvailabilityStrategy(bookingRepository, scheduleSource),
                        new TimeSlotBookingAvailabilityStrategy(bookingRepository, scheduleSource)
                )
        );
    }

    public AvailabilityResult check(
            String businessId,
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile) {

        if (businessId == null || businessId.isBlank()) {
            return unknown("Business ID is required to check booking availability.");
        }
        if (businessProfile == null) {
            return unknown("Business information is not available.");
        }

        RequestDefinition definition = definitionFor(service, businessProfile);
        BookingAvailabilityStrategy strategy = strategyRegistry.strategyFor(definition, fields);

        if (strategy == null) {
            return unknown("No availability strategy is configured for service " + service + ".");
        }

        return strategy.check(
                businessId,
                service,
                fields,
                businessProfile,
                definition
        );
    }

    private RequestDefinition definitionFor(
            String service,
            BusinessProfile profile) {

        if (service == null || profile.services() == null) {
            return null;
        }

        return profile.services()
                .stream()
                .filter(definition ->
                        definition.requestType().equalsIgnoreCase(service))
                .findFirst()
                .orElse(null);
    }

    private AvailabilityResult unknown(String message) {
        return new AvailabilityResult(
                AvailabilityStatus.UNKNOWN,
                message
        );
    }
}
