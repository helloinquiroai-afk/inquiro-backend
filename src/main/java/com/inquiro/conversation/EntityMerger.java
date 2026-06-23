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
                new HashMap<>(existing);

        merged.putAll(incoming);

        return merged;
    }
}