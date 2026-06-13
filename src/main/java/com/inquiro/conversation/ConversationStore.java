package com.inquiro.conversation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationStore {
    private final Map<String, ConversationSession> sessions =
            new ConcurrentHashMap<>();
}
