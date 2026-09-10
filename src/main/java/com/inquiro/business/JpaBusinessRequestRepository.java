package com.inquiro.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaBusinessRequestRepository
        implements BusinessRequestRepository {

    private final BusinessRequestJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public BusinessRequest findByRequestId(
            String requestId) {

        if (requestId == null || requestId.isBlank()) {
            return null;
        }

        return jpaRepository.findById(requestId)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<BusinessRequest> findByBusinessId(
            String businessId) {

        if (businessId == null || businessId.isBlank()) {
            return List.of();
        }

        return jpaRepository
                .findByBusinessId(businessId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<BusinessRequest> findPendingByBusinessId(
            String businessId) {

        if (businessId == null || businessId.isBlank()) {
            return List.of();
        }

        List<BusinessRequestStatus> pendingStatuses =
                List.of(
                        BusinessRequestStatus.PENDING_CONFIRMATION,
                        BusinessRequestStatus.PENDING_REVIEW
                );

        return jpaRepository
                .findByBusinessIdAndStatusIn(
                        businessId,
                        pendingStatuses
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(
            BusinessRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Business request cannot be null"
            );
        }

        String fieldsJson = serializeFields(
                request.fields()
        );

        BusinessRequestEntity entity =
                new BusinessRequestEntity(
                        request.requestId(),
                        request.businessId(),
                        request.customerId(),
                        request.service(),
                        fieldsJson,
                        request.availabilityStatus(),
                        request.status(),
                        request.createdAt()
                );

        jpaRepository.save(entity);
    }

    private BusinessRequest toDomain(
            BusinessRequestEntity entity) {

        return new BusinessRequest(
                entity.getRequestId(),
                entity.getBusinessId(),
                entity.getCustomerId(),
                entity.getService(),
                deserializeFields(entity.getFieldsJson()),
                entity.getAvailabilityStatus(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    private String serializeFields(
            Map<String, Object> fields) {

        if (fields == null) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize business request fields",
                    e
            );
        }
    }

    private Map<String, Object> deserializeFields(
            String fieldsJson) {

        if (fieldsJson == null || fieldsJson.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(
                    fieldsJson,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize business request fields",
                    e
            );
        }
    }
}