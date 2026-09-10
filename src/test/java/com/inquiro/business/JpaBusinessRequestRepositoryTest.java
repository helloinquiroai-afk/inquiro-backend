package com.inquiro.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inquiro.availability.AvailabilityStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        JpaBusinessRequestRepository.class,
        JpaBusinessRequestRepositoryTest.TestConfig.class
})
class JpaBusinessRequestRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private BusinessRequestRepository repository;

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void shouldSaveFindAndQueryBusinessRequests() {

        Instant createdAt = Instant.now();

        BusinessRequest request =
                new BusinessRequest(
                        "request-001",
                        "biz_001",
                        "customer-001",
                        "BRAKE_REPAIR",
                        Map.of(
                                "vehicleType", "SUV",
                                "preferredDate", "2026-09-12",
                                "preferredTime", "10:00 AM"
                        ),
                        AvailabilityStatus.INDICATED,
                        BusinessRequestStatus.PENDING_CONFIRMATION,
                        createdAt
                );

        repository.save(request);

        BusinessRequest loaded =
                repository.findByRequestId("request-001");

        assertThat(loaded).isNotNull();
        assertThat(loaded.requestId())
                .isEqualTo("request-001");
        assertThat(loaded.businessId())
                .isEqualTo("biz_001");
        assertThat(loaded.customerId())
                .isEqualTo("customer-001");
        assertThat(loaded.service())
                .isEqualTo("BRAKE_REPAIR");

        assertThat(loaded.fields())
                .containsEntry("vehicleType", "SUV")
                .containsEntry("preferredDate", "2026-09-12")
                .containsEntry("preferredTime", "10:00 AM");

        assertThat(loaded.availabilityStatus())
                .isEqualTo(AvailabilityStatus.INDICATED);

        assertThat(loaded.status())
                .isEqualTo(
                        BusinessRequestStatus.PENDING_CONFIRMATION
                );

        assertThat(loaded.createdAt())
                .isEqualTo(createdAt);

        List<BusinessRequest> businessRequests =
                repository.findByBusinessId("biz_001");

        assertThat(businessRequests)
                .hasSize(1);

        List<BusinessRequest> pendingRequests =
                repository.findPendingByBusinessId("biz_001");

        assertThat(pendingRequests)
                .hasSize(1);
    }

    @Test
    void shouldReturnBothPendingStatuses() {

        BusinessRequest confirmationRequest =
                new BusinessRequest(
                        "request-confirmation",
                        "biz_001",
                        "customer-001",
                        "BRAKE_REPAIR",
                        Map.of(),
                        AvailabilityStatus.UNKNOWN,
                        BusinessRequestStatus.PENDING_CONFIRMATION,
                        Instant.now()
                );

        BusinessRequest reviewRequest =
                new BusinessRequest(
                        "request-review",
                        "biz_001",
                        "customer-002",
                        "ENGINE_DIAGNOSTIC",
                        Map.of(),
                        AvailabilityStatus.UNKNOWN,
                        BusinessRequestStatus.PENDING_REVIEW,
                        Instant.now()
                );

        BusinessRequest confirmedRequest =
                new BusinessRequest(
                        "request-confirmed",
                        "biz_001",
                        "customer-003",
                        "OIL_CHANGE",
                        Map.of(),
                        AvailabilityStatus.CONFIRMED,
                        BusinessRequestStatus.CONFIRMED,
                        Instant.now()
                );

        repository.save(confirmationRequest);
        repository.save(reviewRequest);
        repository.save(confirmedRequest);

        List<BusinessRequest> pendingRequests =
                repository.findPendingByBusinessId("biz_001");

        assertThat(pendingRequests)
                .hasSize(2)
                .extracting(BusinessRequest::requestId)
                .containsExactlyInAnyOrder(
                        "request-confirmation",
                        "request-review"
                );
    }

    @Test
    void shouldReturnEmptyListForUnknownBusiness() {

        assertThat(
                repository.findByBusinessId("unknown-business")
        ).isEmpty();

        assertThat(
                repository.findPendingByBusinessId("unknown-business")
        ).isEmpty();
    }

    @Test
    void shouldReturnNullForUnknownRequest() {

        assertThat(
                repository.findByRequestId("unknown-request")
        ).isNull();
    }
}
