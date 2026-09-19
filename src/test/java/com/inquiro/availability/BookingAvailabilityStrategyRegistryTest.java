package com.inquiro.availability;

import com.inquiro.request.RequestActionType;
import com.inquiro.request.RequestDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingAvailabilityStrategyRegistryTest {

    private final BookingAvailabilityStrategy timeSlot =
            new StubStrategy("TIME_SLOT");

    private final BookingAvailabilityStrategy dateRange =
            new StubStrategy("DATE_RANGE");

    private final BookingAvailabilityStrategyRegistry registry =
            new BookingAvailabilityStrategyRegistry(
                    List.of(timeSlot, dateRange)
            );

    @Test
    void selectsConfiguredStrategy() {
        RequestDefinition definition = new RequestDefinition(
                "TABLE_RESERVATION",
                "Table reservation",
                List.of("date", "time", "guestCount"),
                Map.of(),
                RequestActionType.BOOKING,
                List.of("customerName", "customerPhone"),
                "TIME_SLOT"
        );

        assertEquals(timeSlot, registry.strategyFor(definition));
    }

    @Test
    void infersDateRangeForLegacyDefinitionWithoutServiceConfiguration() {
        RequestDefinition definition = new RequestDefinition(
                "ROOM_BOOKING",
                "Room booking",
                List.of(),
                Map.of(),
                RequestActionType.BOOKING
        );

        assertEquals(
                dateRange,
                registry.strategyFor(
                        definition,
                        Map.of(
                                "checkInDate", "2026-09-20",
                                "durationNights", 2
                        )
                )
        );
    }

    private record StubStrategy(String id) implements BookingAvailabilityStrategy {
        @Override
        public AvailabilityResult check(
                String businessId,
                String service,
                Map<String, Object> fields,
                com.inquiro.business.BusinessProfile businessProfile) {
            return new AvailabilityResult(
                    AvailabilityStatus.CONFIRMED,
                    "stub"
            );
        }
    }
}
