package com.inquiro.booking;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "booking_inventory",
        uniqueConstraints = @UniqueConstraint(name = "uq_booking_inventory_business_service",
                columnNames = {"business_id", "service"}))
public class BookingInventoryEntity {
    @Id
    @Column(name = "inventory_id", nullable = false, updatable = false)
    private String inventoryId;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "service", nullable = false)
    private String service;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookingInventoryEntity() {}

    public BookingInventoryEntity(String inventoryId, String businessId, String service,
            int capacity, boolean enabled, Instant updatedAt) {
        this.inventoryId = inventoryId;
        this.businessId = businessId;
        this.service = service;
        this.capacity = capacity;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

    public String getInventoryId() { return inventoryId; }
    public String getBusinessId() { return businessId; }
    public String getService() { return service; }
    public int getCapacity() { return capacity; }
    public boolean isEnabled() { return enabled; }
    public Instant getUpdatedAt() { return updatedAt; }
}
