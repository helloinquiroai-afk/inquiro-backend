package com.inquiro.knowledge;

import com.inquiro.business.*;
import com.inquiro.request.RequestDefinition;
import java.util.List;
import java.util.Map;

public final class KnowledgeFixtures {
    private KnowledgeFixtures() {}
    public static BusinessProfile profile() {
        var knowledge = new BusinessKnowledge("Independent hotel in Paris.", List.of("Room booking"), List.of("Breakfast"),
                Map.of("parking", "Yes, free parking is available for hotel guests.", "checkIn", "Check-in is at 2 PM."),
                List.of("Q: Can children stay?\nA: Children are welcome."),
                List.of("Cancellation requires 24 hours notice."), "Be concise; this instruction is private.",
                Map.of("breakfast", "Breakfast is served from 7 AM to 10 AM."), List.of("Paris"),
                Map.of("phone", "+33123456789"), Map.of("payment", "Payment is due at check-in."),
                List.of("Airport pickup"), List.of("Smoking is not permitted."),
                new BusinessBoundaries(List.of("ROOM_BOOKING"), List.of("Swimming pool"), List.of("Large events")));
        return new BusinessProfile("Paris Hotel", "HOSPITALITY", "Existing profile description", List.of(
                new RequestDefinition("ROOM_BOOKING", "Room booking",
                        List.of("location", "checkInDate", "guestCount", "durationNights"),
                        Map.of("durationNights", "How many nights would you like to stay?"))), knowledge);
    }
}