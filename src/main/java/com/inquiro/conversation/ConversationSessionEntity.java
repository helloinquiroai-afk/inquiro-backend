package com.inquiro.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "conversation_session")
public class ConversationSessionEntity {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private String sessionId;

    @Column(name = "inquiry_json", columnDefinition = "CLOB")
    private String inquiryJson;

    @Column(name = "missing_fields_json", columnDefinition = "CLOB")
    private String missingFieldsJson;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    protected ConversationSessionEntity() {
        // JPA
    }

    public ConversationSessionEntity(
            String sessionId,
            String inquiryJson,
            String missingFieldsJson,
            Instant lastUpdated) {

        this.sessionId = sessionId;
        this.inquiryJson = inquiryJson;
        this.missingFieldsJson = missingFieldsJson;
        this.lastUpdated = lastUpdated;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInquiryJson() {
        return inquiryJson;
    }

    public String getMissingFieldsJson() {
        return missingFieldsJson;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}