package com.inquiro.conversation;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationStore {

    private final Map<String, ConversationSession> sessions =
            new ConcurrentHashMap<>();

    public ConversationSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void save(ConversationSession session) {
        sessions.put(
                session.getSessionId(),
                session
        );
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
