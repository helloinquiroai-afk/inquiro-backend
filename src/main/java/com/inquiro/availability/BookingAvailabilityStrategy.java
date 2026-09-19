package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestDefinition;

import java.util.Map;

public interface BookingAvailabilityStrategy {

    String id();

    AvailabilityResult check(
            String businessId,
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile,
            RequestDefinition definition
    );
}
