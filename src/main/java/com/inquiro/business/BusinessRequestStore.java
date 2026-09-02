package com.inquiro.business;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BusinessRequestStore {

    private final Map<String, BusinessRequest> requests =
            new ConcurrentHashMap<>();

    /**
     * Save or update a request.
     */
    public void save(
            BusinessRequest request) {

        requests.put(
                request.requestId(),
                request
        );

        System.out.println(
                "BusinessRequestStore: request saved"
        );
    }

    /**
     * Find request by request ID.
     */
    public BusinessRequest findByRequestId(
            String requestId) {

        return requests.get(requestId);
    }

    /**
     * Get all requests belonging to a business.
     */
    public List<BusinessRequest> findByBusinessId(
            String businessId) {

        List<BusinessRequest> result =
                new ArrayList<>();

        for (BusinessRequest request :
                requests.values()) {

            if (request.businessId()
                    .equals(businessId)) {

                result.add(request);
            }
        }

        return result;
    }

    /**
     * Get pending requests for a business.
     */
    public List<BusinessRequest> findPendingByBusinessId(
            String businessId) {

        List<BusinessRequest> result =
                new ArrayList<>();

        for (BusinessRequest request :
                requests.values()) {

            if (request.businessId()
                    .equals(businessId)
                    &&
                    request.status()
                            == BusinessRequestStatus
                            .PENDING_CONFIRMATION) {

                result.add(request);
            }
        }

        return result;
    }
}