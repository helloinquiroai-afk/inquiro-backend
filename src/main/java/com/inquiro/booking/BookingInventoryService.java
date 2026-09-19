package com.inquiro.booking;

import com.inquiro.auth.TenantAuthorizationService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingInventoryService {
    private final BookingInventoryJpaRepository repository;
    private final TenantAuthorizationService authorization;

    @Transactional
    public BookingInventoryEntity configure(String businessId, String service, int capacity) {
        authorization.requireBusinessWriteAccess(businessId);
        if (service == null || service.isBlank()) throw new IllegalArgumentException("Service is required");
        if (capacity < 1 || capacity > 1_000_000) throw new IllegalArgumentException("Capacity must be between 1 and 1000000");
        BookingInventoryEntity entity = repository.findByBusinessIdAndService(businessId, service)
                .map(existing -> new BookingInventoryEntity(existing.getInventoryId(), businessId, service, capacity, true, Instant.now()))
                .orElseGet(() -> new BookingInventoryEntity("inventory_" + UUID.randomUUID(), businessId, service, capacity, true, Instant.now()));
        return repository.save(entity);
    }

    public int capacityFor(String businessId, String service) {
        return repository.findByBusinessIdAndService(businessId, service)
                .filter(BookingInventoryEntity::isEnabled)
                .map(BookingInventoryEntity::getCapacity)
                .orElse(0);
    }
}
