package com.inquiro.business;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class BusinessRequestRepository {

    private final List<BusinessRequest> requests =
            new ArrayList<>();

    public BusinessRequest save(
            BusinessRequest request) {

        requests.add(request);

        return request;
    }

    public List<BusinessRequest> findByBusinessId(
            String businessId) {

        return requests.stream()
                .filter(request ->
                        request.businessId()
                                .equals(businessId))
                .toList();
    }

    public List<BusinessRequest> findPendingByBusinessId(
            String businessId) {

        return requests.stream()
                .filter(request ->
                        request.businessId()
                                .equals(businessId))
                .filter(request ->
                        request.status()
                                == BusinessRequestStatus
                                .PENDING_CONFIRMATION)
                .toList();
    }

    public List<BusinessRequest> findByCustomerId(
            String customerId) {

        return requests.stream()
                .filter(request ->
                        request.customerId()
                                .equals(customerId))
                .toList();
    }
}
