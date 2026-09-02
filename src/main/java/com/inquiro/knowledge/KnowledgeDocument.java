package com.inquiro.knowledge;

import java.util.Map;

public record KnowledgeDocument(
        KnowledgeSource source,
        String content,
        Map<String, Object> metadata
) {

    public KnowledgeDocument {

        source =
                source == null
                        ? KnowledgeSource.TEXT
                        : source;

        content =
                content == null
                        ? ""
                        : content;

        metadata =
                metadata == null
                        ? Map.of()
                        : Map.copyOf(metadata);
    }

    public static KnowledgeDocument manualForm(
            String content) {

        return new KnowledgeDocument(
                KnowledgeSource.MANUAL_FORM,
                content,
                Map.of()
        );
    }
}
