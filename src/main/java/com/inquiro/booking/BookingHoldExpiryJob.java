package com.inquiro.booking;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingHoldExpiryJob {
    private final BookingJpaRepository repository;

    public BookingHoldExpiryJob(BookingJpaRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${inquiro.booking.hold-expiry-scan-ms:60000}")
    @Transactional
    public void expireDueHolds() {
        List<BookingEntity> holds = repository.findByStatusAndHoldExpiresAtBefore(
                BookingStatus.HOLD, LocalDateTime.now());
        for (BookingEntity booking : holds) booking.setStatus(BookingStatus.EXPIRED);
        if (!holds.isEmpty()) repository.saveAll(holds);
    }
}
