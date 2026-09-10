package com.inquiro.business;

import com.inquiro.availability.AvailabilityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "business_request")
@Getter
public class BusinessRequestEntity {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private String requestId;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "service")
    private String service;

    @Column(name = "fields_json", columnDefinition = "CLOB")
    private String fieldsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    private AvailabilityStatus availabilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BusinessRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BusinessRequestEntity() {
        // JPA
    }

    public BusinessRequestEntity(
            String requestId,
            String businessId,
            String customerId,
            String service,
            String fieldsJson,
            AvailabilityStatus availabilityStatus,
            BusinessRequestStatus status,
            Instant createdAt) {

        this.requestId = requestId;
        this.businessId = businessId;
        this.customerId = customerId;
        this.service = service;
        this.fieldsJson = fieldsJson;
        this.availabilityStatus = availabilityStatus;
        this.status = status;
        this.createdAt = createdAt;
    }
}