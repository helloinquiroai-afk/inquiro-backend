package com.inquiro.conversation;

import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BusinessContextResolverTest {

    private final BusinessContextResolver resolver = new BusinessContextResolver();

    private RequestDefinition roomBooking(String locationSlot) {
        return new RequestDefinition(
                "ROOM_BOOKING",
                "Room booking",
                List.of(locationSlot, "checkInDate", "guestCount")
        );
    }

    private BusinessProfile profileWithLocations(String... locations) {
        return new BusinessProfile(
                "Test Hotel",
                "HOSPITALITY",
                "Hotel",
                List.of(roomBooking("location")),
                new BusinessKnowledge(
                        "Hotel",
                        List.of("ROOM_BOOKING"),
                        List.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        "",
                        Map.of(),
                        List.of(locations),
                        Map.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        null
                )
        );
    }

    @Test
    void resolvesSingleConfiguredLocationWithoutAskingCustomer() {
        RequestDefinition definition = roomBooking("location");
        BusinessProfile profile = profileWithLocations("Paris Grand Hotel");

        Map<String, Object> resolved = resolver.resolve(
                definition,
                Map.of("guestCount", 2),
                profile
        );

        assertEquals("Paris Grand Hotel", resolved.get("location"));
        assertEquals(2, resolved.get("guestCount"));
    }

    @Test
    void doesNotResolveWhenMultipleConfiguredLocationsExist() {
        RequestDefinition definition = roomBooking("location");
        BusinessProfile profile = profileWithLocations(
                "Hotel Riu Sri Lanka",
                "Hotel Riu Maldives",
                "Hotel Riu Dubai"
        );

        Map<String, Object> resolved = resolver.resolve(
                definition,
                Map.of("guestCount", 2),
                profile
        );

        assertFalse(resolved.containsKey("location"));
        assertEquals(2, resolved.get("guestCount"));
    }

    @Test
    void explicitCustomerLocationWinsOverConfiguredContext() {
        RequestDefinition definition = roomBooking("location");
        BusinessProfile profile = profileWithLocations(
                "Hotel Riu Sri Lanka",
                "Hotel Riu Maldives"
        );

        Map<String, Object> resolved = resolver.resolve(
                definition,
                Map.of("location", "Hotel Riu Maldives"),
                profile
        );

        assertEquals("Hotel Riu Maldives", resolved.get("location"));
    }
}
