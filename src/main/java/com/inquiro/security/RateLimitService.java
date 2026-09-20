package com.inquiro.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    private static final long WINDOW_MILLIS = 60_000L;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public Decision tryAcquire(String key, int limit) {
        if (limit <= 0) return new Decision(false, 60);
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= WINDOW_MILLIS) {
                return new Window(now, new AtomicLong(1));
            }
            current.count.incrementAndGet();
            return current;
        });
        long count = window.count.get();
        if (count <= limit) return new Decision(true, secondsUntilReset(window.startedAt, now));
        return new Decision(false, secondsUntilReset(window.startedAt, now));
    }

    public void clear() { windows.clear(); }

    private static long secondsUntilReset(long startedAt, long now) {
        return Math.max(1, Duration.ofMillis(WINDOW_MILLIS - Math.max(0, now - startedAt)).toSeconds());
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {}
    private record Window(long startedAt, AtomicLong count) {}
}
