package com.inquiro.conversation;

public interface ConversationRepository {

    ConversationSession find(String sessionId);

    void save(ConversationSession session);

    void remove(String sessionId);
}
