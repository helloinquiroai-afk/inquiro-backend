package com.inquiro.business;

import com.inquiro.availability.AvailabilityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessRequestService {

    private final BusinessRequestStore businessRequestStore;

    /**
     * Creates a normal business request.
     *
     * The request starts as PENDING_CONFIRMATION.
     */
    public BusinessRequest create(
            String businessId,
            String customerId,
            String service,
            Map<String, Object> fields,
            AvailabilityStatus availabilityStatus) {

        return createRequest(
                businessId,
                customerId,
                service,
                fields,
                availabilityStatus,
                BusinessRequestStatus.PENDING_CONFIRMATION
        );
    }

    /**
     * Creates a request that requires manual business review.
     */
    public BusinessRequest createForHumanReview(
            String businessId,
            String customerId,
            String service,
            Map<String, Object> fields) {

        return createRequest(
                businessId,
                customerId,
                service,
                fields,
                AvailabilityStatus.UNKNOWN,
                BusinessRequestStatus.PENDING_REVIEW
        );
    }

    /**
     * Common request creation logic.
     */
    private BusinessRequest createRequest(
            String businessId,
            String customerId,
            String service,
            Map<String, Object> fields,
            AvailabilityStatus availabilityStatus,
            BusinessRequestStatus status) {

        String requestId =
                UUID.randomUUID().toString();

        BusinessRequest request =
                new BusinessRequest(
                        requestId,
                        businessId,
                        customerId,
                        service,
                        fields,
                        availabilityStatus,
                        status,
                        Instant.now()
                );

        businessRequestStore.save(request);

        return request;
    }

    /**
     * Confirm a pending request.
     */
    public BusinessRequest confirm(
            String requestId) {

        BusinessRequest request =
                businessRequestStore.findByRequestId(
                        requestId
                );

        if (request == null) {

            throw new IllegalArgumentException(
                    "Business request not found: "
                            + requestId
            );
        }

        if (request.status()
                != BusinessRequestStatus.PENDING_CONFIRMATION
                && request.status()
                != BusinessRequestStatus.PENDING_REVIEW) {

            throw new IllegalStateException(
                    "Request cannot be confirmed. Current status: "
                            + request.status()
            );
        }

        BusinessRequest confirmed =
                new BusinessRequest(
                        request.requestId(),
                        request.businessId(),
                        request.customerId(),
                        request.service(),
                        request.fields(),
                        request.availabilityStatus(),
                        BusinessRequestStatus.CONFIRMED,
                        request.createdAt()
                );

        businessRequestStore.save(confirmed);

        return confirmed;
    }

    /**
     * Reject a pending request.
     */
    public BusinessRequest reject(
            String requestId) {

        BusinessRequest request =
                businessRequestStore.findByRequestId(
                        requestId
                );

        if (request == null) {

            throw new IllegalArgumentException(
                    "Business request not found: "
                            + requestId
            );
        }

        if (request.status()
                != BusinessRequestStatus.PENDING_CONFIRMATION
                && request.status()
                != BusinessRequestStatus.PENDING_REVIEW) {

            throw new IllegalStateException(
                    "Request cannot be rejected. Current status: "
                            + request.status()
            );
        }

        BusinessRequest rejected =
                new BusinessRequest(
                        request.requestId(),
                        request.businessId(),
                        request.customerId(),
                        request.service(),
                        request.fields(),
                        request.availabilityStatus(),
                        BusinessRequestStatus.REJECTED,
                        request.createdAt()
                );

        businessRequestStore.save(rejected);

        return rejected;
    }
}