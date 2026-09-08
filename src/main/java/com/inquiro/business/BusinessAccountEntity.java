package com.inquiro.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_account")
public class BusinessAccountEntity {

    @Id
    @Column(name = "business_id", nullable = false, updatable = false)
    private String businessId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "description")
    private String description;

    protected BusinessAccountEntity() {
        // JPA
    }

    public BusinessAccountEntity(
            String businessId,
            String businessName,
            String businessType,
            String description) {

        this.businessId = businessId;
        this.businessName = businessName;
        this.businessType = businessType;
        this.description = description;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getDescription() {
        return description;
    }
}