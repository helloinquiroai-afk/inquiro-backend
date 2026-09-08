
package com.inquiro.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_channel")
public class BusinessChannelEntity {

    @Id
    @Column(name = "channel_id", nullable = false, updatable = false)
    private String channelId;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected BusinessChannelEntity() {
        // JPA
    }

    public BusinessChannelEntity(
            String channelId,
            String businessId,
            String type,
            String externalId,
            boolean enabled) {

        this.channelId = channelId;
        this.businessId = businessId;
        this.type = type;
        this.externalId = externalId;
        this.enabled = enabled;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getType() {
        return type;
    }

    public String getExternalId() {
        return externalId;
    }

    public boolean isEnabled() {
        return enabled;
    }
}