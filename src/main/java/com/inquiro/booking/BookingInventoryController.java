package com.inquiro.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business/accounts/{businessId}/booking-inventory")
@RequiredArgsConstructor
public class BookingInventoryController {
    private final BookingInventoryService inventory;

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse configure(
            @PathVariable String businessId,
            @Valid @RequestBody InventoryRequest request) {
        BookingInventoryEntity entity = inventory.configure(businessId, request.service(), request.capacity());
        return new InventoryResponse(entity.getInventoryId(), entity.getBusinessId(), entity.getService(),
                entity.getCapacity(), entity.isEnabled());
    }

    public record InventoryRequest(@NotBlank String service, @Min(1) int capacity) {}
    public record InventoryResponse(String inventoryId, String businessId, String service, int capacity, boolean enabled) {}
}
