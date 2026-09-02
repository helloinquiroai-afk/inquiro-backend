package com.inquiro.business;

import com.inquiro.availability.AvailabilityStatus;

import java.time.Instant;
import java.util.Map;

public record BusinessRequest(

        String requestId,

        String businessId,

        String customerId,

        String service,

        Map<String, Object> fields,

        AvailabilityStatus availabilityStatus,

        BusinessRequestStatus status,

        Instant createdAt

) {
}