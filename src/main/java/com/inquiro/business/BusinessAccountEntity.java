
package com.inquiro.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_account")
public class BusinessAccountEntity {

    @Id
    @Column(
            name = "business_id",
            nullable = false,
            updatable = false
    )
    private String businessId;

    @Column(
            name = "business_name",
            nullable = false
    )
    private String businessName;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "description")
    private String description;

    @Column(
            name = "profile_json",
            columnDefinition = "CLOB"
    )
    private String profileJson;

    protected BusinessAccountEntity() {
        // JPA
    }

    public BusinessAccountEntity(
            String businessId,
            String businessName,
            String businessType,
            String description,
            String profileJson) {

        this.businessId = businessId;
        this.businessName = businessName;
        this.businessType = businessType;
        this.description = description;
        this.profileJson = profileJson;
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

    public String getProfileJson() {
        return profileJson;
    }

    public void setBusinessName(
            String businessName) {

        this.businessName = businessName;
    }

    public void setBusinessType(
            String businessType) {

        this.businessType = businessType;
    }

    public void setDescription(
            String description) {

        this.description = description;
    }

    public void setProfileJson(
            String profileJson) {

        this.profileJson = profileJson;
    }
}