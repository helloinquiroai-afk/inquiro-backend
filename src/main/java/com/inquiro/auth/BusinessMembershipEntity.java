package com.inquiro.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "business_membership", uniqueConstraints =
        @UniqueConstraint(name = "uq_business_membership_user_business", columnNames = {"user_id", "business_id"}))
public class BusinessMembershipEntity {
    @Id
    @Column(name = "membership_id", nullable = false, updatable = false, length = 64)
    private String membershipId;

    @Column(name = "user_id", nullable = false, updatable = false, length = 64)
    private String userId;

    @Column(name = "business_id", nullable = false, updatable = false, length = 128)
    private String businessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private BusinessMembershipRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BusinessMembershipEntity() {
        // JPA
    }

    public BusinessMembershipEntity(String membershipId, String userId, String businessId,
                                    BusinessMembershipRole role, Instant createdAt) {
        this.membershipId = membershipId;
        this.userId = userId;
        this.businessId = businessId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getMembershipId() { return membershipId; }
    public String getUserId() { return userId; }
    public String getBusinessId() { return businessId; }
    public BusinessMembershipRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
