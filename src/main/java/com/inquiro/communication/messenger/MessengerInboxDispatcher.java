package com.inquiro.communication.messenger;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "messenger.worker-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MessengerInboxDispatcher {
    private final MessengerInboxRepository inbox;
    private final MessengerInboxWorker worker;

    // One scheduled consumer preserves receive order for the single-instance MVP.
    @Scheduled(fixedDelayString = "${messenger.poll-interval-ms:1000}")
    public void dispatch() {
        try {
            var pending = inbox.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                    List.of(MessengerInboxEvent.Status.RECEIVED, MessengerInboxEvent.Status.REPLY_READY), Instant.now());
            for (var event : pending) {
                try {
                    worker.prepare(event.getId());
                    worker.deliver(event.getId());
                } catch (RuntimeException exception) {
                    log.error("event=messenger_worker_failure inbox_id={} error_type={}",
                            event.getId(), exception.getClass().getSimpleName());
                }
            }
        } catch (RuntimeException exception) {
            log.error("event=messenger_poll_failure error_type={}", exception.getClass().getSimpleName());
        }
    }
}
