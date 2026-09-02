package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;

import java.util.Map;

public interface AvailabilitySource {

    AvailabilityResult check(
            String service,
            Map<String, Object> fields,
            BusinessProfile businessProfile
    );
}
