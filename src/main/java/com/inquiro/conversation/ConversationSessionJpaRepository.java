package com.inquiro.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationSessionJpaRepository
        extends JpaRepository<ConversationSessionEntity, String> {
}
