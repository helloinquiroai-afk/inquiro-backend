package com.inquiro.booking;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingInventoryJpaRepository extends JpaRepository<BookingInventoryEntity, String> {
    Optional<BookingInventoryEntity> findByBusinessIdAndService(String businessId, String service);
}
