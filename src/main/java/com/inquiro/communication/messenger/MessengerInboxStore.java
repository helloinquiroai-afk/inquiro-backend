package com.inquiro.communication.messenger;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class MessengerInboxStore {
    private final EntityManager entityManager;

    // A duplicate must roll back only its own insert, never other events in the batch.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(MessengerInboxEvent event) {
        entityManager.persist(event);
        entityManager.flush();
    }
}
