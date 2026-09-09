package com.inquiro.conversation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaConversationRepository
        implements ConversationRepository {

    private final ConversationSessionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ConversationSession find(String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        return jpaRepository
                .findById(sessionId)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public void save(ConversationSession session) {

        if (session == null) {
            throw new IllegalArgumentException(
                    "Conversation session cannot be null"
            );
        }

        String inquiryJson =
                serialize(session.getInquiry());

        String missingFieldsJson =
                serialize(session.getMissingFields());

        ConversationSessionEntity entity =
                new ConversationSessionEntity(
                        session.getSessionId(),
                        inquiryJson,
                        missingFieldsJson,
                        session.getLastUpdated()
                );

        jpaRepository.save(entity);
    }

    @Override
    public void remove(String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        jpaRepository.deleteById(sessionId);
    }

    private ConversationSession toDomain(
            ConversationSessionEntity entity) {

        return new ConversationSession(
                entity.getSessionId(),
                deserializeInquiry(
                        entity.getInquiryJson()
                ),
                deserializeMissingFields(
                        entity.getMissingFieldsJson()
                ),
                entity.getLastUpdated()
        );
    }

    private String serialize(Object value) {

        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize conversation data",
                    e
            );
        }
    }

    private com.inquiro.inquiry.InquiryResult deserializeInquiry(
            String json) {

        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(
                    json,
                    com.inquiro.inquiry.InquiryResult.class
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize inquiry",
                    e
            );
        }
    }

    private List<String> deserializeMissingFields(
            String json) {

        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(
                                    List.class,
                                    String.class
                            )
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize missing fields",
                    e
            );
        }
    }
}
