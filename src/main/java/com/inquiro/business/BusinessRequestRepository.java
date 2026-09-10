package com.inquiro.business;

import java.util.List;

public interface BusinessRequestRepository {

    BusinessRequest findByRequestId(
            String requestId);

    List<BusinessRequest> findByBusinessId(
            String businessId);

    List<BusinessRequest> findPendingByBusinessId(
            String businessId);

    void save(
            BusinessRequest request);
}