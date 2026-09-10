package com.inquiro.conversation;

import java.util.HashMap;
import java.util.Map;

public final class EntityMerger {

    private EntityMerger() {
    }

    public static Map<String, Object> merge(
            Map<String, Object> existing,
            Map<String, Object> incoming) {

        Map<String, Object> merged =
                new HashMap<>(existing == null ? Map.of() : existing);

        if (incoming != null) {
            incoming.forEach((key, value) -> {
                if (value != null && !String.valueOf(value).isBlank()) merged.put(key, value);
            });
        }

        return merged;
    }
}
