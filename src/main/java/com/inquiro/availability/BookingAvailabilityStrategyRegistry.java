package com.inquiro.availability;

import com.inquiro.request.RequestDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BookingAvailabilityStrategyRegistry {

    private final Map<String, BookingAvailabilityStrategy> strategies;

    public BookingAvailabilityStrategyRegistry(List<BookingAvailabilityStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        strategy -> strategy.id().toUpperCase(),
                        Function.identity()
                ));
    }

    public BookingAvailabilityStrategy strategyFor(RequestDefinition definition) {
        if (definition == null || definition.availabilityStrategy() == null) {
            return null;
        }
        return strategies.get(definition.availabilityStrategy().toUpperCase());
    }

    public BookingAvailabilityStrategy strategyFor(String id) {
        if (id == null || id.isBlank()) return null;
        return strategies.get(id.toUpperCase());
    }
}
