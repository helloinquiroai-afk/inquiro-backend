package com.inquiro.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.inquiry.InquiryResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        JpaConversationRepository.class,
        JpaConversationRepositoryTest.TestConfig.class
})
class JpaConversationRepositoryTest {

    @Autowired
    private ConversationRepository repository;

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void shouldSaveFindAndRemoveConversation() {

        InquiryResult inquiry = new InquiryResult(
                "AUTOMOTIVE",
                "VEHICLE_SERVICE",
                Map.of(
                        "vehicleType", "SUV",
                        "serviceType", "Brake repair"
                )
        );

        Instant lastUpdated = Instant.now();

        ConversationSession session = new ConversationSession(
                "session-001",
                inquiry,
                List.of("date", "time"),
                lastUpdated
        );

        // Save
        repository.save(session);

        // Find
        ConversationSession loaded =
                repository.find("session-001");

        assertThat(loaded).isNotNull();

        assertThat(loaded.getSessionId())
                .isEqualTo("session-001");

        assertThat(loaded.getInquiry())
                .isNotNull();

        assertThat(loaded.getInquiry().domain())
                .isEqualTo("AUTOMOTIVE");

        assertThat(loaded.getInquiry().service())
                .isEqualTo("VEHICLE_SERVICE");

        assertThat(loaded.getInquiry().fields())
                .containsEntry("vehicleType", "SUV")
                .containsEntry("serviceType", "Brake repair");

        assertThat(loaded.getMissingFields())
                .containsExactly("date", "time");

        assertThat(loaded.getLastUpdated())
                .isEqualTo(lastUpdated);

        // Remove
        repository.remove("session-001");

        // Verify removed
        assertThat(repository.find("session-001"))
                .isNull();
    }
}