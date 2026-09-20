package com.inquiro.conversation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowUpFieldResolverTest {

    @Test
    void capturesSimpleNameWhenAiReturnsNoEntity() {
        Map<String, Object> result = FollowUpFieldResolver.resolve(
                Map.of(),
                "Gayan",
                List.of("customerName")
        );

        assertEquals("Gayan", result.get("customerName"));
    }

    @Test
    void capturesPhoneNumberWhenAiReturnsNoEntity() {
        Map<String, Object> result = FollowUpFieldResolver.resolve(
                Map.of(),
                "0713157890",
                List.of("customerPhone")
        );

        assertEquals("0713157890", result.get("customerPhone"));
    }

    @Test
    void doesNotTreatConversationPhraseAsName() {
        Map<String, Object> result = FollowUpFieldResolver.resolve(
                Map.of(),
                "already provided",
                List.of("customerName")
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void preservesAiExtractedValue() {
        Map<String, Object> result = FollowUpFieldResolver.resolve(
                Map.of("customerName", "John"),
                "Gayan",
                List.of("customerName")
        );

        assertEquals("John", result.get("customerName"));
    }
}
