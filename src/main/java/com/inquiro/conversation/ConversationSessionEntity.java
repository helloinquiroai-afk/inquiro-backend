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

    @Column(name = "business_id")
    private String businessId;

    @Column(name = "channel_type")
    private String channelType;

    @Column(name = "external_channel_id")
    private String externalChannelId;

    @Column(name = "customer_id")
    private String customerId;

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
        ConversationIdentity identity = ConversationIdentity.fromSessionId(sessionId);
        if (identity != null) {
            this.businessId = identity.businessId();
            this.channelType = identity.channel().name();
            this.externalChannelId = identity.externalId();
            this.customerId = identity.customerId();
        }
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
