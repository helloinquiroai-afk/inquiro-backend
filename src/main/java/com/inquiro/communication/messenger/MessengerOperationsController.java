package com.inquiro.communication.messenger;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Operator-only endpoints protected by ManagementAccessFilter. Payloads and credentials are never returned. */
@RestController
@RequestMapping("/api/business/accounts/{businessId}/messenger/events")
@RequiredArgsConstructor
public class MessengerOperationsController {
    private final MessengerInboxRepository inbox;

    @GetMapping
    public List<EventStatus> list(@PathVariable String businessId,
            @RequestParam(defaultValue = "FAILED") MessengerInboxEvent.Status status) {
        return inbox.findTop50ByBusinessIdAndStatusOrderByIdDesc(businessId, status).stream()
                .map(EventStatus::from).toList();
    }

    @PostMapping("/{eventId}/retry")
    @Transactional
    public EventStatus retry(@PathVariable String businessId, @PathVariable Long eventId) {
        var event = inbox.lockById(eventId).filter(item -> item.getBusinessId().equals(businessId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (event.getStatus() != MessengerInboxEvent.Status.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        event.setStatus(event.getReply() == null ? MessengerInboxEvent.Status.RECEIVED : MessengerInboxEvent.Status.REPLY_READY);
        event.setSendAttempts(0);
        event.setFailureCode(null);
        event.setNextAttemptAt(Instant.now());
        return EventStatus.from(event);
    }

    public record EventStatus(Long id, String businessId, MessengerInboxEvent.Status status,
                              Instant receivedAt, int sendAttempts, String failureCode) {
        static EventStatus from(MessengerInboxEvent event) {
            return new EventStatus(event.getId(), event.getBusinessId(), event.getStatus(), event.getReceivedAt(),
                    event.getSendAttempts(), event.getFailureCode());
        }
    }
}
