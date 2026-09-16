package com.inquiro.availability;

import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessScheduleAvailabilitySourceTest {

    private final BusinessScheduleAvailabilitySource source =
            new BusinessScheduleAvailabilitySource();

    @Test
    void shouldConfirmAvailabilityDuringBusinessHours() {

        BusinessProfile profile =
                profileWithHours(
                        Map.of(
                                "MONDAY", "08:00-18:00",
                                "TUESDAY", "08:00-18:00",
                                "WEDNESDAY", "08:00-18:00",
                                "THURSDAY", "08:00-18:00",
                                "FRIDAY", "08:00-18:00",
                                "SATURDAY", "08:00-13:00",
                                "SUNDAY", "CLOSED"
                        )
                );

        AvailabilityResult result =
                source.check(
                        "VEHICLE_INSPECTION",
                        Map.of(
                                "date", "2026-09-16",
                                "time", "10:00"
                        ),
                        profile
                );

        assertEquals(
                AvailabilityStatus.CONFIRMED,
                result.status()
        );
    }

    @Test
    void shouldRejectAvailabilityOutsideBusinessHours() {

        BusinessProfile profile =
                profileWithHours(
                        Map.of(
                                "WEDNESDAY", "08:00-18:00"
                        )
                );

        AvailabilityResult result =
                source.check(
                        "VEHICLE_INSPECTION",
                        Map.of(
                                "date", "2026-09-16",
                                "time", "20:00"
                        ),
                        profile
                );

        assertEquals(
                AvailabilityStatus.UNAVAILABLE,
                result.status()
        );
    }

    @Test
    void shouldRejectAvailabilityWhenBusinessIsClosed() {

        BusinessProfile profile =
                profileWithHours(
                        Map.of(
                                "SUNDAY", "CLOSED"
                        )
                );

        AvailabilityResult result =
                source.check(
                        "VEHICLE_INSPECTION",
                        Map.of(
                                "date", "2026-09-20",
                                "time", "10:00"
                        ),
                        profile
                );

        assertEquals(
                AvailabilityStatus.UNAVAILABLE,
                result.status()
        );
    }

    @Test
    void shouldReturnUnknownWhenOperatingHoursAreNotConfigured() {

        BusinessProfile profile =
                profileWithHours(Map.of());

        AvailabilityResult result =
                source.check(
                        "VEHICLE_INSPECTION",
                        Map.of(
                                "date", "2026-09-16",
                                "time", "10:00"
                        ),
                        profile
                );

        assertEquals(
                AvailabilityStatus.UNKNOWN,
                result.status()
        );
    }

    @Test
    void shouldReturnUnknownWhenDateIsMissing() {

        BusinessProfile profile =
                profileWithHours(
                        Map.of(
                                "WEDNESDAY", "08:00-18:00"
                        )
                );

        AvailabilityResult result =
                source.check(
                        "VEHICLE_INSPECTION",
                        Map.of(
                                "time", "10:00"
                        ),
                        profile
                );

        assertEquals(
                AvailabilityStatus.UNKNOWN,
                result.status()
        );
    }

    @Test
    void shouldReturnUnknownWhenTimeIsMissing() {

        BusinessProfile profile =
                profileWithHours(
                        Map.of(
                                "WEDNESDAY", "08:00-18:00"
                        )
                );

        AvailabilityResult result =
                source.check(
                        "VEHICLE_INSPECTION",
                        Map.of(
                                "date", "2026-09-16"
                        ),
                        profile
                );

        assertEquals(
                AvailabilityStatus.UNKNOWN,
                result.status()
        );
    }

    private BusinessProfile profileWithHours(
            Map<String, String> operatingHours) {

        BusinessKnowledge knowledge =
                new BusinessKnowledge(
                        "Auto care business",
                        List.of("Vehicle inspection"),
                        List.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        "",
                        operatingHours,
                        List.of(),
                        Map.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        null
                );

        RequestDefinition service =
                new RequestDefinition(
                        "VEHICLE_INSPECTION",
                        "Vehicle inspection",
                        List.of(
                                "date",
                                "time"
                        )
                );

        return new BusinessProfile(
                "ABC Auto Care",
                "AUTOMOTIVE",
                "Auto care business",
                List.of(service),
                knowledge
        );
    }
}