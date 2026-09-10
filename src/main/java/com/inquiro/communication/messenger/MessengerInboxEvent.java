package com.inquiro.communication.messenger;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "messenger_inbox", uniqueConstraints = @UniqueConstraint(name = "uq_messenger_event_key", columnNames = "event_key"),
        indexes = @Index(name = "idx_messenger_pending", columnList = "status,next_attempt_at,id"))
@Getter
@Setter
@NoArgsConstructor
public class MessengerInboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_key", nullable = false, length = 64, updatable = false)
    private String eventKey;
    @Column(name = "business_id", nullable = false, updatable = false)
    private String businessId;
    @Column(nullable = false, updatable = false)
    private String pageId;
    @Column(nullable = false, updatable = false)
    private String senderId;
    @Column(length = 512, updatable = false)
    private String messageId;
    private long eventTimestamp;
    @Column(length = 10000, nullable = false)
    private String messageText;
    @Column(length = 10000)
    private String reply;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.RECEIVED;
    @Column(nullable = false)
    private Instant receivedAt = Instant.now();
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();
    private int sendAttempts;
    private String failureCode;

    public enum Status { RECEIVED, REPLY_READY, SENT, FAILED }

    public MessengerInboxEvent(String key, String businessId, MessengerEvent event) {
        this.eventKey = key;
        this.businessId = businessId;
        this.pageId = event.pageId();
        this.senderId = event.senderId();
        this.messageId = event.messageId();
        this.eventTimestamp = event.timestamp();
        this.messageText = event.text();
    }
}
