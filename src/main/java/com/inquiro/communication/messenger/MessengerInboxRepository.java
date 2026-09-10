package com.inquiro.communication.messenger;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MessengerInboxRepository extends JpaRepository<MessengerInboxEvent, Long> {
    boolean existsByEventKey(String eventKey);

    List<MessengerInboxEvent> findTop50ByBusinessIdAndStatusOrderByIdDesc(
            String businessId, MessengerInboxEvent.Status status);

    List<MessengerInboxEvent> findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
            Collection<MessengerInboxEvent.Status> statuses, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MessengerInboxEvent e where e.id = :id")
    Optional<MessengerInboxEvent> lockById(Long id);
}
