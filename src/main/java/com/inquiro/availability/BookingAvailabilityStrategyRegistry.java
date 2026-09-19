package com.inquiro.availability;

import com.inquiro.request.RequestDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BookingAvailabilityStrategyRegistry {

    private final Map<String, BookingAvailabilityStrategy> strategies;

    public BookingAvailabilityStrategyRegistry(List<BookingAvailabilityStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        strategy -> strategy.id().toUpperCase(Locale.ROOT),
                        Function.identity()
                ));
    }

    public BookingAvailabilityStrategy strategyFor(RequestDefinition definition) {
        if (definition == null || definition.availabilityStrategy() == null) {
            return null;
        }
        return strategyFor(definition.availabilityStrategy());
    }

    public BookingAvailabilityStrategy strategyFor(
            RequestDefinition definition,
            Map<String, Object> fields) {

        if (definition != null
                && definition.availabilityStrategy() != null
                && !"AUTO".equalsIgnoreCase(definition.availabilityStrategy())) {
            BookingAvailabilityStrategy configured = strategyFor(definition);
            if (configured != null) {
                return configured;
            }
        }

        if (fields != null &&
                (fields.containsKey("durationNights")
                        || fields.containsKey("checkOutDate")
                        || fields.containsKey("checkInDate"))) {
            return strategyFor("DATE_RANGE");
        }

        if (definition != null && definition.requiredSlots() != null
                && definition.requiredSlots().stream().anyMatch(slot ->
                "checkInDate".equalsIgnoreCase(slot)
                        || "checkOutDate".equalsIgnoreCase(slot)
                        || "durationNights".equalsIgnoreCase(slot))) {
            return strategyFor("DATE_RANGE");
        }

        return strategyFor("TIME_SLOT");
    }

    public BookingAvailabilityStrategy strategyFor(String id) {
        if (id == null || id.isBlank()) return null;
        return strategies.get(id.toUpperCase(Locale.ROOT));
    }
}
